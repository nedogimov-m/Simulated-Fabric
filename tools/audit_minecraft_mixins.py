#!/usr/bin/env python3
"""Read-only member audit against real Minecraft bytecode; not a Mixin injection test."""
import argparse
import json
import os
from pathlib import Path
import re
import struct
import zipfile


class Reader:
    def __init__(self, data): self.data, self.offset = data, 0
    def number(self, size):
        value = int.from_bytes(self.data[self.offset:self.offset + size], "big")
        self.offset += size
        return value
    def skip(self, size): self.offset += size


def class_members(data):
    r = Reader(data)
    if r.number(4) != 0xCAFEBABE: raise ValueError("Invalid class file")
    r.skip(4)
    pool = [None] * r.number(2)
    index = 1
    while index < len(pool):
        tag = r.number(1)
        if tag == 1:
            size = r.number(2)
            pool[index] = r.data[r.offset:r.offset + size].decode("utf-8", errors="replace")
            r.skip(size)
        elif tag == 7: pool[index] = r.number(2)
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): r.skip(4)
        elif tag in (5, 6): r.skip(8); index += 1
        elif tag in (8, 16, 19, 20): r.skip(2)
        elif tag == 15: r.skip(3)
        else: raise ValueError(f"Unknown constant pool tag {tag}")
        index += 1
    r.skip(2)
    r.skip(2)
    superclass = r.number(2)
    parent = pool[pool[superclass]] if superclass else None
    interfaces = [pool[pool[r.number(2)]] for _ in range(r.number(2))]
    groups = []
    for _ in range(2):
        members = set()
        for _ in range(r.number(2)):
            r.skip(2)
            name, descriptor = pool[r.number(2)], pool[r.number(2)]
            members.add((name, descriptor))
            for _ in range(r.number(2)):
                r.skip(2)
                r.skip(r.number(4))
        groups.append(members)
    return {"parent": parent, "interfaces": interfaces, "fields": groups[0], "methods": groups[1]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True)
    parser.add_argument("--include-client", action="store_true")
    parser.add_argument("--jar", type=Path, default=Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) /
                        "caches/fabric-loom/26.3/minecraft-merged.jar")
    args = parser.parse_args()
    root = args.project.resolve()
    issues, skipped, cache = [], [], {}
    counters = {"mixins": 0, "injection_selectors": 0, "invoke_targets": 0, "shadow_names": 0, "access_widener_entries": 0}
    with zipfile.ZipFile(args.jar) as jar:
        def members(owner):
            if owner not in cache:
                name = owner + ".class"
                cache[owner] = class_members(jar.read(name)) if name in jar.namelist() else None
            return cache[owner]

        def has(owner, group, name, descriptor=None, inherited=False):
            visited = set()
            remaining = [owner]
            while remaining:
                owner = remaining.pop()
                if not owner or owner in visited: continue
                visited.add(owner)
                value = members(owner)
                if value is None: continue
                if any(n == name and (descriptor is None or d == descriptor) for n, d in value[group]): return True
                if inherited:
                    remaining.extend([value["parent"], *value["interfaces"]])
            return False

        def issue(path, kind, detail): issues.append({"source": str(path.relative_to(root)), "kind": kind, "detail": detail})

        for loader in ("common", "fabric"):
            java = root / loader / "src/main/java"
            resources = root / loader / "src/main/resources"
            for config in resources.glob("*.mixins.json"):
                data = json.loads(config.read_text())
                names = data.get("mixins", []) + data.get("server", [])
                if args.include_client: names += data.get("client", [])
                for name in names:
                    path = java / (data["package"].replace(".", "/") + "/" + name.replace(".", "/") + ".java")
                    if not path.is_file(): issue(config, "missing_source", str(path)); continue
                    source = re.sub(r"/\*.*?\*/|//[^\n]*", "", path.read_text(), flags=re.S)
                    imported = {item.rsplit(".", 1)[-1]: item for item in re.findall(r"^import\s+([\w.$]+);", source, re.M)}
                    annotation = re.search(r"@Mixin\s*\((.*?)\)\s*", source, re.S)
                    if not annotation: skipped.append(str(path.relative_to(root))); continue
                    targets = []
                    for target in re.findall(r"([\w.$]+)\.class", annotation[1]):
                        qualified = imported.get(target, target)
                        if qualified.startswith("net.minecraft."):
                            owner = qualified.replace(".", "/")
                            # Imported nested classes use the JVM '$' separator.
                            while members(owner) is None and "/" in owner:
                                at = owner.rfind("/")
                                owner = owner[:at] + "$" + owner[at + 1:]
                            targets.append(owner)
                    targets += [target.replace(".", "/") for target in re.findall(r'"(net\.minecraft\.[^"]+)"', annotation[1])]
                    if not targets: skipped.append(str(path.relative_to(root))); continue
                    counters["mixins"] += 1
                    for owner in targets:
                        if members(owner) is None: issue(path, "missing_target_class", owner); continue
                        for declaration in re.findall(r'\bmethod\s*=\s*(\{[^}]*\}|"[^"]+")', source, re.S):
                            for selector in re.findall(r'"([^"]+)"', declaration):
                                counters["injection_selectors"] += 1
                                if any(char in selector for char in "*?") or selector.startswith("L"): continue
                                method, _, signature = selector.partition("(")
                                if not has(owner, "methods", method, "(" + signature if signature else None):
                                    issue(path, "missing_injection_selector", owner + ";" + selector)
                        for declaration in re.findall(r"@Shadow(?:\([^)]*\))?\s*((?:@[\w.]+(?:\([^)]*\))?\s*)*[^;{}]+)[;{]", source):
                            before = declaration.split("=", 1)[0]
                            if "(" in before:
                                match = re.search(r"(\w+)\s*\(", before)
                                group = "methods"
                            else:
                                match = re.search(r"(\w+)\s*$", before.strip())
                                group = "fields"
                            if match:
                                counters["shadow_names"] += 1
                                if not has(owner, group, match[1], inherited=True):
                                    issue(path, "missing_shadow_name", owner + ";" + match[1])
                    for owner, member in re.findall(r'\btarget\s*=\s*"L(net/minecraft/[^;]+);([^" ]+)"', source):
                        counters["invoke_targets"] += 1
                        if "(" in member:
                            name, descriptor = member.split("(", 1)
                            ok = has(owner, "methods", name, "(" + descriptor, inherited=True)
                        elif ":" in member:
                            name, descriptor = member.split(":", 1)
                            ok = has(owner, "fields", name, descriptor, inherited=True)
                        else: continue
                        if not ok: issue(path, "missing_instruction_target", owner + ";" + member)
            for aw in resources.glob("*.accesswidener"):
                for line in aw.read_text().splitlines():
                    parts = line.split("#", 1)[0].split()
                    if len(parts) < 3 or parts[1] not in ("class", "field", "method"): continue
                    kind, owner = parts[1:3]
                    if not owner.startswith("net/minecraft/"): continue
                    counters["access_widener_entries"] += 1
                    if kind == "class": ok = members(owner) is not None
                    else: ok = len(parts) >= 5 and has(owner, kind + "s", parts[3], parts[4], inherited=True)
                    if not ok: issue(aw, "missing_access_widener_target", line)
    output = root / "build/diagnostic/minecraft-mixin-audit.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({"jar": str(args.jar), "scope": "all" if args.include_client else "common/server",
                                  "counters": counters, "issues": issues, "skipped_non_minecraft": skipped,
                                  "limitations": "Bytecode name/descriptor checks only; no injection/cardinality/runtime proof. Shadow types and non-Minecraft dependencies require further audit."}, indent=2) + "\n")
    from collections import Counter
    print(json.dumps({"checked": counters, "issues": dict(Counter(x["kind"] for x in issues)), "report": str(output)}))


if __name__ == "__main__": main()
