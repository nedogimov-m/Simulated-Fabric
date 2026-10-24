#!/usr/bin/env python3
"""Check retired legacy datagen provenance and byte-preserved native model/blockstate inputs."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT / 'ports/simulated/retired-blockstate-datagen.json').read_text())
for entry in manifest['files']:
    assert not (ROOT / entry['retired']).exists(), entry['retired']
    assert hashlib.sha256((ROOT / entry['archive']).read_bytes()).hexdigest() == entry['head_sha256'], entry['archive']
for path, digest in manifest['generated_assets_preserved'].items():
    assert hashlib.sha256((ROOT / path).read_bytes()).hexdigest() == digest, path
for side in ['common', 'fabric']:
    for path in (ROOT / 'simulated' / side / 'src/main/java').rglob('*.java'):
        source = path.read_text()
        assert 'SimBlockStateGen' not in source and 'SimBlockStateService' not in source, path
print(json.dumps({'status': 'PASS', 'archived_legacy_datagen_sources': len(manifest['files']),
                  'preserved_source_models_and_blockstates': manifest['generated_asset_count'],
                  'runtime_reference_scope': 'Fabric/common production sources'}))
