#!/usr/bin/env python3
"""Make a compatible copy of the known airship schematic without changing its blocks."""
import gzip
import hashlib
import json
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]


def main():
    original = ROOT / 'build/porting/airshipbase/airshipbase.original.nbt'
    output = original.with_name('airshipbase.compat.nbt')
    source_sha = hashlib.sha256(original.read_bytes()).hexdigest()
    assert source_sha == 'e85f70a6fffc3458ebfad4ee244483b654c12bed4b709e4540ee0575742c6fe6'
    decoded = json.loads(original.with_name('decoded.json').read_text())
    changed = []
    for block in decoded['blocks']:
        if decoded['palette'][block['state']]['Name'] != 'create:redstone_link':
            continue
        for field in ('FrequencyFirst', 'FrequencyLast'):
            if block.get('nbt', {}).get(field, {}).get('id') == 'aeroscapes:aerolite_twig':
                changed.append({'sourceRelative': block['pos'], 'field': field})
    assert changed == [
        {'sourceRelative': [18, 24, 71], 'field': 'FrequencyFirst'},
        {'sourceRelative': [18, 24, 71], 'field': 'FrequencyLast'},
        {'sourceRelative': [19, 37, 52], 'field': 'FrequencyFirst'},
        {'sourceRelative': [19, 37, 52], 'field': 'FrequencyLast'},
    ]
    def utf(value):
        raw = value.encode('utf-8')
        return struct.pack('>H', len(raw)) + raw
    raw = gzip.decompress(original.read_bytes())
    before, after = utf('aeroscapes:aerolite_twig'), utf('minecraft:paper')
    assert raw.count(before) == 4 and raw.count(after) == 0
    repaired = raw.replace(before, after)
    assert repaired.count(after) == 4 and repaired.replace(after, before) == raw
    output.write_bytes(gzip.compress(repaired, mtime=0))
    assert gzip.decompress(output.read_bytes()) == repaired
    proof = {'sourceSha256': source_sha, 'outputSha256': hashlib.sha256(output.read_bytes()).hexdigest(),
             'changes': changed, 'oldItem': 'aeroscapes:aerolite_twig', 'newItem': 'minecraft:paper',
             'invariant': 'Only the four length-prefixed NBT item-ID strings changed; reversing those changes reproduces the exact original uncompressed bytes. All block positions, states, entities, inventories and other NBT are unchanged.'}
    output.with_suffix('.json').write_text(json.dumps(proof, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'output': str(output.relative_to(ROOT)), 'sha256': proof['outputSha256'], 'changedStrings': 4}))


if __name__ == '__main__':
    main()
