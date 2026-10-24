#!/usr/bin/env python3
"""Read authored Ponder NBT palettes and validate against an actual native block-registry export."""
from pathlib import Path
import argparse
import gzip
import json
import struct

ROOT = Path(__file__).resolve().parents[1]


class Tag:
    def __init__(self, kind, value, start, payload_start, end):
        self.kind, self.value = kind, value
        self.start, self.payload_start, self.end = start, payload_start, end


class Reader:
    def __init__(self, data):
        self.data, self.offset = data, 0

    def number(self, fmt):
        value = struct.unpack_from('>' + fmt, self.data, self.offset)[0]
        self.offset += struct.calcsize('>' + fmt)
        return value

    def string(self):
        length = self.number('H')
        value = self.data[self.offset:self.offset + length].decode('utf-8')
        self.offset += length
        return value

    def tag(self, kind):
        start = self.offset
        if kind in range(1, 7):
            value = self.number({1: 'b', 2: 'h', 3: 'i', 4: 'q', 5: 'f', 6: 'd'}[kind])
        elif kind == 8:
            value = self.string()
        elif kind in (7, 11, 12):
            size = self.number('i') * {7: 1, 11: 4, 12: 8}[kind]
            value = self.data[self.offset:self.offset + size]
            self.offset += size
        elif kind == 9:
            child = self.number('b')
            value = [self.tag(child) for _ in range(self.number('i'))]
        elif kind == 10:
            value = {}
            while True:
                named_start = self.offset
                child = self.number('b')
                if child == 0:
                    break
                name = self.string()
                item = self.tag(child)
                item.start = named_start
                value[name] = item
        else:
            raise ValueError('Unsupported NBT tag ' + str(kind))
        return Tag(kind, value, start, start, self.offset)


def read_nbt(path):
    source = path.read_bytes()
    data = gzip.decompress(source) if source[:2] == b'\x1f\x8b' else source
    reader = Reader(data)
    kind = reader.number('b')
    reader.string()
    tree = reader.tag(kind)
    if reader.offset != len(data):
        raise ValueError('Trailing NBT data: ' + str(path))
    return data, tree


def palettes(tree):
    fields = tree.value
    if 'palette' in fields:
        return [fields['palette'].value]
    return [palette.value for palette in fields.get('palettes', Tag(9, [], 0, 0, 0)).value]


def inspect(path, registry=None, block_entities=None):
    _, tree = read_nbt(path)
    blocks = tree.value.get('blocks', Tag(9, [], 0, 0, 0)).value
    entries, issues = [], []
    for palette_index, palette in enumerate(palettes(tree)):
        for state_index, entry in enumerate(palette):
            fields = entry.value
            name = fields['Name'].value
            properties = {key: value.value for key, value in fields.get('Properties', Tag(10, {}, 0, 0, 0)).value.items()}
            positions = [[coordinate.value for coordinate in block.value['pos'].value]
                         for block in blocks if block.value['state'].value == state_index]
            detail = {'palette': palette_index, 'state': state_index, 'name': name,
                      'properties': properties, 'positions': positions}
            entries.append(detail)
            if registry is None:
                continue
            if name not in registry:
                issues.append({**detail, 'problem': 'unregistered_block'})
            else:
                for key, value in properties.items():
                    if key not in registry[name] or value not in registry[name][key]:
                        issues.append({**detail, 'problem': 'invalid_property', 'key': key, 'value': value,
                                       'allowed': registry[name].get(key)})
    saved_entities = []
    first_palette = palettes(tree)[0] if palettes(tree) else []
    for block in blocks:
        data = block.value.get('nbt')
        if data is None:
            continue
        position = [coordinate.value for coordinate in block.value['pos'].value]
        state = block.value['state'].value
        owner = first_palette[state].value['Name'].value
        type_id = data.value.get('id')
        detail = {'position': position, 'block': owner, 'type': type_id.value if type_id else None}
        saved_entities.append(detail)
        if block_entities is not None:
            if type_id is None or type_id.value not in block_entities:
                issues.append({**detail, 'problem': 'unregistered_block_entity'})
            elif owner not in block_entities[type_id.value]:
                issues.append({**detail, 'problem': 'invalid_block_entity_owner'})
    return {'asset': str(path.relative_to(ROOT)), 'block_count': len(blocks), 'entries': entries,
            'block_entities': saved_entities, 'issues': issues}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--registry', type=Path, help='Actual flat block IDs -> property names -> allowed values JSON.')
    parser.add_argument('--block-entities', type=Path, help='Actual block-entity IDs -> valid owner block IDs JSON.')
    parser.add_argument('--report', type=Path, default=ROOT / 'build/porting/ponder-palettes/native-audit.json')
    args = parser.parse_args()
    registry = json.loads(args.registry.read_text()) if args.registry else None
    block_entities = json.loads(args.block_entities.read_text()) if args.block_entities else None
    files = [inspect(path, registry, block_entities) for mod in ('simulated', 'aeronautics', 'offroad')
             for loader in ('common', 'fabric') for source_set in ('main', 'generated')
             for path in sorted((ROOT / mod / loader / 'src' / source_set / 'resources/assets').glob('*/ponder/**/*.nbt'))]
    issues = sum(len(item['issues']) for item in files)
    report = {'scope': 'Read-only authored NBT palette audit; no block-state replacement or ignored property.',
              'registry': str(args.registry) if args.registry else None, 'assets': len(files),
              'block_entity_registry': str(args.block_entities) if args.block_entities else None,
              'palette_entries': sum(len(item['entries']) for item in files), 'issues': issues, 'files': files}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({key: report[key] for key in ('assets', 'palette_entries', 'issues', 'registry')}))
    if (registry is not None or block_entities is not None) and issues:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
