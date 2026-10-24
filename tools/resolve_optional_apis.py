#!/usr/bin/env python3
"""Resolve the exact upstream optional API jars already recorded by the port audit."""
import hashlib
import json
from pathlib import Path
import urllib.request

directory = Path(__file__).resolve().parents[1] / 'ports/optional-api'
manifest = json.loads((directory / 'artifacts.json').read_text())
count = 0
for artifact in manifest['artifacts']:
    if artifact['role'] != 'compile-only':
        continue
    path = directory / artifact['file']
    if path.is_file():
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if digest != artifact['sha256']:
            raise SystemExit(f'Optional API hash mismatch: {path}; expected {artifact["sha256"]}, found {digest}')
    else:
        request = urllib.request.Request(artifact['url'], headers={'User-Agent':'Fabric26.3-port-build'})
        with urllib.request.urlopen(request, timeout=60) as response:
            content = response.read()
        if hashlib.sha256(content).hexdigest() != artifact['sha256']:
            raise SystemExit(f'Upstream bytes do not match the pinned API: {artifact["url"]}')
        temporary = path.with_suffix('.download')
        temporary.write_bytes(content)
        temporary.replace(path)
    count += 1
print(f'Genuine optional API targets: {count} pinned jars ready; no runtime mods added.')
