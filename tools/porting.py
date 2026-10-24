#!/usr/bin/env python3
"""Cheap, read-only port inventory and validation of real dependency artifacts."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import zipfile


ROOT = Path(__file__).resolve().parents[1]
CREATE = ROOT.parent / "26_3_porting"
DEPENDENCIES = {
    "create": ("create", CREATE / "build/libs", "create-fabric-6.0.10-port.2+mc26.3.jar"),
    "companion": ("sablecompanion", ROOT.parent / "sable/companion/build/libs",
                  "sable-companion-fabric-26.3-1.6.0-port.1+mc26.3.jar"),
    "veil": ("veil", ROOT.parent / "veil/build/libs", "veil-fabric-26.3-port.jar"),
    "sable": ("sable", ROOT.parent / "sable/build/libs", "sable-fabric-26.3-port.jar"),
}


def metadata(path):
    with zipfile.ZipFile(path) as jar:
        return json.loads(jar.read("fabric.mod.json"))


def artifacts(overrides):
    supplied = {}
    for item in overrides:
        name, separator, raw_path = item.partition("=")
        if not separator or name not in DEPENDENCIES or not raw_path:
            raise ValueError(f"Expected --artifact name=path; names: {', '.join(DEPENDENCIES)}")
        if name in supplied:
            raise ValueError(f"Duplicate override: {name}")
        path = Path(raw_path).expanduser()
        supplied[name] = (ROOT / path).resolve() if not path.is_absolute() else path.resolve()
    result = {}
    for name, (mod_id, directory, default_name) in DEPENDENCIES.items():
        if name in supplied:
            result[name] = supplied[name]
            continue
        preferred = directory / default_name
        if preferred.is_file():
            result[name] = preferred
            continue
        candidates = []
        for path in sorted(directory.glob("*.jar")):
            if path.name.endswith(("-sources.jar", "-javadoc.jar", "-dev.jar")):
                continue
            try:
                if metadata(path).get("id") == mod_id:
                    candidates.append(path)
            except (OSError, KeyError, ValueError, zipfile.BadZipFile):
                continue
        if len(candidates) > 1:
            raise ValueError(f"Ambiguous {name} artifacts; pass --artifact {name}=<exact jar>")
        result[name] = candidates[0] if candidates else preferred
    return result


def targets_26_3(predicate):
    # Intentionally accept explicit 26.3 constraints only. Broad '*' and old
    # versions provide no evidence that a dependency is a port for this target.
    if isinstance(predicate, list):
        return any(targets_26_3(item) for item in predicate)
    if not isinstance(predicate, str):
        return False
    clauses = predicate.split()
    if not clauses:
        return False
    exact = {"26.3", "=26.3", "~26.3", "~26.3.0"}
    if len(clauses) == 1:
        return predicate in exact
    return clauses[0] in {">=26.3", ">=26.3.0"} and clauses[1:] in (["<26.4"], ["<26.4.0"])


def verify(overrides):
    errors = []
    records = []
    for name, path in artifacts(overrides).items():
        if not path.is_file():
            errors.append(f"{name}: missing {path}")
            continue
        try:
            with zipfile.ZipFile(path) as jar:
                data = json.loads(jar.read("fabric.mod.json"))
                expected_id = DEPENDENCIES[name][0]
                if data.get("id") != expected_id:
                    errors.append(f"{name}: expected mod id {expected_id!r}, found {data.get('id')!r}")
                minecraft = data.get("depends", {}).get("minecraft")
                if not targets_26_3(minecraft):
                    errors.append(f"{name}: expected explicit Minecraft 26.3 constraint, found {minecraft!r}")
                classes = [entry for entry in jar.infolist()
                           if entry.filename.endswith(".class") and not entry.filename.startswith("META-INF/versions/")]
                if not classes:
                    errors.append(f"{name}: artifact contains no compiled classes")
                for entries in data.get("entrypoints", {}).values():
                    for entrypoint in entries:
                        if isinstance(entrypoint, dict):
                            if entrypoint.get("adapter", "default") != "default":
                                continue
                            entrypoint = entrypoint.get("value", "")
                        if not isinstance(entrypoint, str):
                            errors.append(f"{name}: malformed entrypoint {entrypoint!r}")
                            continue
                        owner = entrypoint.split("::", 1)[0].replace(".", "/") + ".class"
                        if owner not in jar.namelist():
                            errors.append(f"{name}: declared entrypoint has no compiled class: {entrypoint}")
                for entry in classes:
                    with jar.open(entry) as stream:
                        header = stream.read(8)
                    if len(header) != 8 or header[:4] != b"\xca\xfe\xba\xbe":
                        errors.append(f"{name}: invalid class file {entry.filename}")
                        break
                    major = int.from_bytes(header[6:8], "big")
                    if major > 69:
                        errors.append(f"{name}: {entry.filename} needs Java {major - 44}; target Java is 25")
                        break
                with path.open("rb") as stream:
                    digest = hashlib.file_digest(stream, "sha256").hexdigest()
                records.append({"dependency": name, "path": str(path), "version": data.get("version"),
                                "sha256": digest, "classes": len(classes)})
        except (OSError, KeyError, ValueError, zipfile.BadZipFile) as error:
            errors.append(f"{name}: cannot validate Fabric jar: {error}")
    for record in records:
        print(f"{record['dependency']}: {record['version']}, {record['classes']} classes, SHA256 {record['sha256']}")
    if errors:
        print("Dependency gate failed:", file=sys.stderr)
        for error in errors:
            print(f"  {error}", file=sys.stderr)
        return 1
    output = ROOT / "build/porting/dependencies.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(records, indent=2) + "\n")
    print("Artifact prerequisites passed; gameplay and runtime remain separate checks.")
    return 0


def audit():
    if not (CREATE / "src").is_dir():
        raise ValueError(f"Create source baseline missing: {CREATE}")
    target_classes = set()
    for directory in (CREATE / "src/main/java", CREATE / "src/client/java"):
        for path in directory.rglob("*.java"):
            target_classes.add(".".join(path.relative_to(directory).with_suffix("").parts))
    prefixes = {"create": "com.simibubi.create.", "catnip": "net.createmod.catnip.",
                "ponder": "net.createmod.ponder.", "flywheel": "dev.engine_room.flywheel.",
                "registrate": "com.tterrag.registrate."}
    used = {name: set() for name in prefixes}
    report = {"baseline": str(CREATE), "modules": {}, "external_usage": {"sable": 0, "veil": 0},
              "common_neoforge_files": [], "api": {}}
    for module in ("simulated", "aeronautics", "offroad"):
        counts = {}
        for loader in ("common", "neoforge", "fabric"):
            paths = sorted((ROOT / module / loader / "src/main/java").rglob("*.java"))
            counts[loader] = len(paths)
            if loader != "common":
                continue
            for path in paths:
                source = path.read_text()
                for dependency, prefix in (("sable", "dev.ryanhcode.sable."), ("veil", "foundry.veil.")):
                    if prefix in source:
                        report["external_usage"][dependency] += 1
                if re.search(r"^import (?:static )?net\.neoforged\.", source, re.MULTILINE):
                    report["common_neoforge_files"].append(str(path.relative_to(ROOT)))
                for imported in re.findall(r"^import\s+([^\s;]+);", source, re.MULTILINE):
                    if imported.endswith(".*"):
                        continue
                    for name, prefix in prefixes.items():
                        if imported.startswith(prefix):
                            used[name].add(imported)
        report["modules"][module] = counts
    for name, imported in used.items():
        missing = []
        for symbol in sorted(imported):
            owner = symbol
            while owner not in target_classes and "." in owner:
                owner = owner.rsplit(".", 1)[0]
            if owner not in target_classes:
                missing.append(symbol)
        report["api"][name] = {"imported_classes": len(imported), "missing_classes": missing}
    output = ROOT / "build/porting/audit.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n")
    for module, counts in report["modules"].items():
        print(f"{module}: " + ", ".join(f"{loader}={count}" for loader, count in counts.items()))
    for name, data in report["api"].items():
        print(f"{name}: {len(data['missing_classes'])}/{data['imported_classes']} imported class paths absent from Create baseline")
    print(f"Common files importing NeoForge: {len(report['common_neoforge_files'])}")
    print(f"External usage (common files): {report['external_usage']}")
    print(f"Details: {output}")
    return 0


def status():
    """Compare recorded successful checks' inputs; never rerun those checks."""
    ledger = json.loads((ROOT / "PORTING_VERIFIED.json").read_text())
    changed = False
    for name, scope in ledger["scopes"].items():
        differences = []
        for relative, expected in scope["files"].items():
            path = ROOT / relative
            if not path.is_file():
                differences.append(relative)
                continue
            with path.open("rb") as stream:
                actual = hashlib.file_digest(stream, "sha256").hexdigest()
            if actual != expected:
                differences.append(relative)
        for pattern in scope.get("watch_globs", []):
            for path in ROOT.glob(pattern):
                relative = str(path.relative_to(ROOT))
                if path.is_file() and relative not in scope["files"] and relative not in differences:
                    differences.append(relative)
        changed |= bool(differences)
        if differences:
            print(f"{name}: changed inputs ({len(differences)}); reassess the affected checks")
            for path in differences:
                print(f"  {path}")
        else:
            print(f"{name}: recorded inputs unchanged; {scope['evidence']}")
    print("This compares recorded inputs only; it does not certify the complete mod or run tests.")
    return 1 if changed else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("audit", "paths", "verify-deps", "status"))
    parser.add_argument("--artifact", action="append", default=[], metavar="NAME=PATH")
    args = parser.parse_args()
    try:
        if args.command == "audit":
            return audit()
        if args.command == "status":
            return status()
        if args.command == "paths":
            for path in artifacts(args.artifact).values():
                print(path)
            return 0
        return verify(args.artifact)
    except ValueError as error:
        print(error, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
