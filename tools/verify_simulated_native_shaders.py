#!/usr/bin/env python3
"""Compile only three new Simulated shader sources and optionally run their narrow Vulkan fixture."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shlex
import subprocess
import zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--client', action='store_true')
args = parser.parse_args()
java = Path(os.environ['JAVA_HOME']) / 'bin'
work = root / 'tools/build/simulated-native-shaders'
classes = work / 'classes'
classes.mkdir(parents=True, exist_ok=True)
native = root / 'ports/veil/build/rendertype-verification'
gpu = root / 'ports/veil/build/gpu-verification'
if not (native / 'native-rendertype-acceptance.jar').is_file():
    raise SystemExit('Prepare the genuine native Veil render-type component boundary first.')
launcher = shlex.split((native / 'client.args').read_text())
cp_index = launcher.index('-classpath') + 1
classpath = launcher[cp_index].split(os.pathsep)
sources = [root / name for name in [
    'simulated/common/src/main/java/dev/simulated_team/simulated/index/SimRenderTypes.java',
    'simulated/fabric/src/main/java/dev/simulated_team/simulated/fabric/client/NativeSimWorldShaderBindings.java',
    'simulated/fabric/src/main/java/dev/simulated_team/simulated/fabric/mixin/client/NativeShaderFrameMixin.java',
    'tools/port-tests/SimNativeShaderGpuCheck.java']]
compile_cp = os.pathsep.join([str(native / 'classes'), str(gpu / 'classes'), *classpath])
result = subprocess.run([str(java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', '', '-classpath', compile_cp,
                         '-d', str(classes), *map(str, sources)], capture_output=True, text=True)
(work / 'compile.log').write_text(result.stdout + result.stderr)
print('New Simulated native shaders: 3 genuine production sources + 1 explicit fixture; javac exit', result.returncode, flush=True)
if result.returncode:
    print(result.stdout + result.stderr)
    raise SystemExit(result.returncode)
assets = root / 'simulated/common/src/main/resources/assets/simulated'
inputs = {'scope': 'seven production shader pairs/eight native pipelines and exact new shader pixels; not complete Simulated/Veil startup',
          'sources': {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest() for path in [*sources, Path(__file__).resolve()]},
          'resources': {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest() for path in (assets / 'shaders/native').rglob('*') if path.is_file()},
          'boundaries': {str(folder): {str(path.relative_to(folder)): hashlib.sha256(path.read_bytes()).hexdigest() for path in sorted(folder.rglob('*.class'))}
                         for folder in [native / 'classes', gpu / 'classes']}}
inputs['wiring'] = {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest() for path in [
    root / 'simulated/common/src/main/java/dev/simulated_team/simulated/SimulatedClient.java',
    root / 'simulated/fabric/src/main/resources/simulated.native_render.mixins.json']}
(work / 'inputs.json').write_text(json.dumps(inputs, indent=2) + '\n')
if not args.client:
    raise SystemExit(0)
jarpath = work / 'simulated-native-shaders-acceptance.jar'
with zipfile.ZipFile(native / 'native-rendertype-acceptance.jar') as source, zipfile.ZipFile(jarpath, 'w', zipfile.ZIP_DEFLATED) as target:
    for entry in source.infolist():
        if entry.filename == 'fabric.mod.json' or entry.filename.endswith('.class'):
            continue
        target.writestr(entry, source.read(entry))
    target.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'simulated_native_shader_acceptance', 'version': '1.0.0', 'environment': 'client',
        'name': 'Simulated native shader acceptance (test only)', 'entrypoints': {'main': ['SimNativeShaderGpuCheck'], 'client': ['SimNativeShaderGpuCheck']},
        'mixins': ['native-rendertype-test.mixins.json', 'native-world-bridge.mixins.json', 'simulated-frame.mixins.json'],
        'accessWidener': 'native-rendertype.accesswidener', 'depends': {'minecraft': '26.3', 'fabricloader': '>=0.19.5', 'fabric-api': '*'}}))
    target.writestr('native-world-bridge.mixins.json', json.dumps({'required': True, 'minVersion': '0.8', 'package': 'foundry.veil.mixin.shader.client', 'compatibilityLevel': 'JAVA_25',
        'client': ['NativePipelineCacheMixin', 'NativeShaderManagerMixin', 'NativeFrontendRenderPassMixin'], 'injectors': {'defaultRequire': 1}}))
    target.writestr('simulated-frame.mixins.json', json.dumps({'required': True, 'minVersion': '0.8', 'package': 'dev.simulated_team.simulated.fabric.mixin.client', 'compatibilityLevel': 'JAVA_25',
        'client': ['NativeShaderFrameMixin'], 'injectors': {'defaultRequire': 1}}))
    seen = set()
    for folder in [classes, gpu / 'classes', native / 'classes']:
        for path in folder.rglob('*.class'):
            name = str(path.relative_to(folder))
            if name not in seen:
                target.write(path, name)
                seen.add(name)
    for path in (assets / 'shaders/native').rglob('*'):
        if path.is_file(): target.write(path, 'assets/simulated/' + str(path.relative_to(assets)))
    target.write(root / 'LICENSE.md', 'LICENSE_Simulated')
launcher[cp_index] = os.pathsep.join([str(jarpath) if path == str(native / 'native-rendertype-acceptance.jar') else path for path in classpath])
run = work / 'run'
run.mkdir(exist_ok=True)
launcher[launcher.index('--gameDir') + 1] = str(run)
report = work / 'runtime.json'
report.unlink(missing_ok=True)
launcher = [arg for arg in launcher if not arg.startswith('-Dveil.rendertype.')]
launcher.insert(0, '-Dsimulated.native.shaders.report=' + str(report))
argfile = work / 'client.args'
argfile.write_text('\n'.join('"' + arg.replace('\\', '\\\\').replace('"', '\\"') + '"' for arg in launcher))
print('Launching only the new Simulated native shader acceptance on actual Vulkan.', flush=True)
result = subprocess.run([str(java / 'java'), '@' + str(argfile)], cwd=run, timeout=180)
if result.returncode: raise SystemExit(result.returncode)
if not report.is_file(): raise SystemExit('Simulated native shader acceptance produced no report')
data = json.loads(report.read_text())
if data.get('status') != 'passed': raise SystemExit('Simulated native shader acceptance failed: ' + str(data))
print('Simulated native shader acceptance passed:', json.dumps(data))
