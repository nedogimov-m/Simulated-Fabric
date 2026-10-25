#!/usr/bin/env python3
"""Launch real production Fabric jars; the separate driver contains no production classes."""
import argparse
import atexit
import hashlib
import io
import json
import os
from pathlib import Path
import shlex
import shutil
import subprocess
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path(os.environ.get('JAVA_HOME', '/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home')) / 'bin'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--client-args', type=Path, help='Existing native Fabric client launch arguments for this machine.')
parser.add_argument('--side', choices=['server', 'client'], required=True)
parser.add_argument('--name', required=True)
parser.add_argument('--mod', type=Path, action='append', required=True)
parser.add_argument('--driver-entrypoint', help='External test-only FabricClientGameTest entrypoint; production classes remain in the frozen JARs.')
parser.add_argument('--driver-source', type=Path, action='append', default=[], help='Additional external test-only Java source, used with --driver-entrypoint.')
parser.add_argument('--driver-resources', type=Path, action='append', default=[], help='Test resource directory; production assets cannot be replaced.')
parser.add_argument('--driver-property', action='append', default=[], help='JVM key=value passed to an external test driver.')
parser.add_argument('--hand-depth', action='store_true', help='Run the new hand-light depth regression; omit for already verified boundaries.')
parser.add_argument('--skip-activation', action='store_true', help='Reuse the separately recorded successful target application scope when its hooks are unchanged.')
parser.add_argument('--services-only', action='store_true', help='Check only the new fuel/glue/assembly provider boundary in a genuine server world.')
parser.add_argument('--services-checks', choices=['all','assembly'], default='all')
parser.add_argument('--client-checks', choices=['all','ponder','endsea-visible','aeronautics-flight','item-resources','reported-crash','ponder-block-entities','borehead','warning-resources','user-regressions','gameplay-audit'], default='all', help='Resume only the unverified client scenes after an independent successful scope was preserved.')
parser.add_argument('--audit-checks', choices=['items','placement','ponder','tools'], default='items', help='Native addon inventory/model, finite-stock placement/use or full addon storyboard audit.')
parser.add_argument('--audit-item', help='Investigate one exact registered item without repeating accepted item/placement rows.')
parser.add_argument('--audit-case', help='Investigate one native tool-use scenario without replaying accepted cases.')
parser.add_argument('--crash-checks', choices=['all','ponder','none'], default='all', help='Select only the new reported crash boundary; none isolates item resources and ponder reuses a separately accepted statistics scope.')
parser.add_argument('--item-gui-only', action='store_true', help='Resume only the unverified EN/RU item GUI screenshots within reported-crash; accepted names, baked partials and hands are not repeated.')
parser.add_argument('--ponder-owners-only', action='store_true', help='Check only the two newly migrated Create Ponder owner types; accepted portable-engine placement and restore is not repeated.')
parser.add_argument('--keep-open', action='store_true', help='Keep the aeronautics-flight world open until its helper receives the control file.')
parser.add_argument('--direction-checks', choices=['gearshift','analog','borehead','swivel','torsion'], help='Run only the selected real dedicated-server direction matrix.')
parser.add_argument('--direction-phase', choices=['initial','restore'], default='initial', help='Restore uses the same real world after the previous JVM saved and exited.')
parser.add_argument('--direction-tick-rate', type=float, default=200, help='Logical server ticks per second for the new direction matrix; no previous scopes are selected.')
parser.add_argument('--warning-checks', choices=['valid','invalid'], default='valid', help='Isolate only malformed-model fallback after accepted valid tire/reload coverage.')
parser.add_argument('--regression-checks', choices=['diagram','schematicannon','schematicannon-interaction','physical-walking','glue','grass','create-walking','create-micro-walking','plot-interaction','assembler-lifecycle','assembler-roundtrip','airship-operation','airship-bootstrap','airship-preheat-launch','airship-refuel','airship-reload','airship-propulsion','plot-water','airship-recovery','create-plot-retain','bundle-startup','fabric-cleanup'], default='diagram')
parser.add_argument('--physical-accepted-static', type=Path, help='Reuse the exact separately accepted static walking trace; run only unverified moving cases.')
parser.add_argument('--physical-rotation', action='store_true', help='Check only new tilted platform cases, retaining both accepted aligned walking traces.')
parser.add_argument('--create-support-probes-only', action='store_true', help='Check the changed native support probe contract without repeating the accepted standing and walk-off trace.')
parser.add_argument('--airship-operation-accepted', type=Path, help='Resume only airship controls from the existing accepted assembly/motion report and saved world.')
parser.add_argument('--airship-controls-accepted', type=Path, help='Retain already accepted individual airship controls and run only the remaining cases.')
parser.add_argument('--cannon-mode', choices=['prefix','mini','full'], default='prefix')
parser.add_argument('--resume-world', type=Path, help='Open the actual previously saved client test world in a new JVM.')
options = parser.parse_args()
external_driver = bool(options.driver_entrypoint)
if external_driver and (options.side != 'client' or not options.driver_source):
    parser.error('--driver-entrypoint requires --side client and at least one --driver-source')
if not external_driver and (options.driver_source or options.driver_resources or options.driver_property):
    parser.error('External driver inputs require --driver-entrypoint')
for value in options.driver_property:
    if '=' not in value or not value.split('=', 1)[0] or value.startswith('-'):
        parser.error('--driver-property requires key=value')
direction_matrix = options.direction_checks is not None
if direction_matrix and options.side != 'server': parser.error('--direction-checks requires --side server')
if options.keep_open and (options.side != 'client' or options.client_checks != 'aeronautics-flight'):
    parser.error('--keep-open requires --side client --client-checks aeronautics-flight')
if options.client_checks == 'aeronautics-flight' and options.side != 'client':
    parser.error('--client-checks aeronautics-flight requires --side client')
if options.client_checks in ('item-resources','reported-crash','ponder-block-entities','borehead','warning-resources','user-regressions','gameplay-audit') and options.side != 'client':
    parser.error('Targeted resource/Ponder checks require --side client')
if options.item_gui_only and (options.side != 'client' or options.client_checks != 'reported-crash'):
    parser.error('--item-gui-only requires --side client --client-checks reported-crash')
if options.ponder_owners_only and (options.side != 'client' or options.client_checks != 'ponder-block-entities'):
    parser.error('--ponder-owners-only requires --side client --client-checks ponder-block-entities')
if options.client_checks == 'reported-crash' and options.crash_checks == 'none' and not options.item_gui_only:
    parser.error('--client-checks reported-crash requires --crash-checks all or ponder')
item_resources = options.side == 'client' and options.client_checks == 'item-resources'
reported_crash = options.side == 'client' and options.client_checks == 'reported-crash'
ponder_block_entities = options.side == 'client' and options.client_checks == 'ponder-block-entities'
borehead = options.side == 'client' and options.client_checks == 'borehead'
warning_resources = options.side == 'client' and options.client_checks == 'warning-resources'
user_regressions = options.side == 'client' and options.client_checks == 'user-regressions'
gameplay_audit = options.side == 'client' and options.client_checks == 'gameplay-audit'
collision_probe = user_regressions and options.regression_checks in ('airship-reload', 'airship-propulsion')
isolated_client = item_resources or reported_crash or ponder_block_entities or borehead or warning_resources or user_regressions or gameplay_audit or external_driver

mods = [p.resolve() for p in options.mod]
work = ROOT / 'build/porting/packaged-smoke' / options.name / options.side
work.mkdir(parents=True, exist_ok=True)
if (work/'inputs.json').is_file():
    history = work/'attempts'
    history.mkdir(exist_ok=True)
    previous = history/str(len(list(history.iterdir()))+1)
    previous.mkdir()
    for name in ['inputs.json', 'runtime.json', 'activation.json', 'runtime.log', 'compile.log', 'launch.args', 'mod-inputs', 'packaged-smoke-driver.jar', 'driver-classes', 'driver-sources']:
        path = work/name
        if path.exists(): shutil.move(str(path), str(previous/name))
snapshot_dir = work/'mod-inputs'
snapshot_dir.mkdir(exist_ok=True)

def discard_finished_binary_snapshots():
    """Retain input identities and results, rather than old production builds."""
    removed = []
    for path in sorted(snapshot_dir.glob('*.jar')):
        if not path.is_file():
            continue
        removed.append({'file': str(path.relative_to(ROOT)), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                        'bytes': path.stat().st_size})
        path.unlink()
    if removed:
        (work/'artifact-retention.json').write_text(json.dumps({
            'policy': 'User requested latest builds only; temporary runtime JAR snapshots discarded after the process exited.',
            'historicalInputsUnchanged': True, 'removedBinarySnapshots': removed,
        }, indent=2)+'\n')

atexit.register(discard_finished_binary_snapshots)
snapshots = {}
mod_hashes = {}
nested_compile = []
mod_roots = {'veil':'../veil', 'sable':'../sable', 'simulated':'simulated', 'aeronautics':'aeronautics', 'offroad':'offroad'}
installed = set()
packaged = {}
production_classes = set()
production_assets = set()
def inventory(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as jar:
        production_classes.update(name for name in jar.namelist() if name.endswith('.class'))
        production_assets.update(name for name in jar.namelist() if name.startswith(('assets/', 'data/')))
        if 'fabric.mod.json' not in jar.namelist(): return
        metadata = json.loads(jar.read('fabric.mod.json'))
        installed.add(metadata['id'])
        classes = set()
        for entry in metadata.get('mixins', []):
            if isinstance(entry, dict):
                if entry.get('environment', '*') not in ('*', options.side): continue
                entry = entry['config']
            config = json.loads(jar.read(entry))
            for side in ['mixins', options.side]:
                classes.update(config['package']+'.'+short for short in config.get(side, []))
        packaged[metadata['id']] = classes
        for entry in metadata.get('jars', []):
            child = jar.read(entry['file'])
            child_path = snapshot_dir/(hashlib.sha256(child).hexdigest()[:12]+'-'+Path(entry['file']).name)
            child_path.write_bytes(child)
            if child_path not in nested_compile: nested_compile.append(child_path)
            inventory(child)
for p in mods:
    # Parallel Loom builds may briefly be replacing the output. Freeze only
    # a complete genuine jar; all compilation and launch then use these bytes.
    for attempt in range(40):
        try:
            raw = p.read_bytes()
            with zipfile.ZipFile(io.BytesIO(raw)) as candidate:
                if candidate.testzip() is not None: raise zipfile.BadZipFile('Incomplete artifact')
            break
        except (FileNotFoundError, zipfile.BadZipFile):
            if attempt == 39: raise
            time.sleep(.25)
    with zipfile.ZipFile(io.BytesIO(raw)) as jar:
        data = json.loads(jar.read('fabric.mod.json'))
        if 'acceptance' in data['id'] or 'fixture' in data['id']:
            raise SystemExit(f'Expected production mod, found fixture {p}')
    frozen = snapshot_dir/p.name
    frozen.write_bytes(raw)
    snapshots[p] = frozen
    mod_hashes[str(p.relative_to(ROOT)) if p.is_relative_to(ROOT) else str(p)] = hashlib.sha256(raw).hexdigest()
    inventory(raw)
targets = set()
for mod_id, classes_for_side in packaged.items():
    if mod_id not in mod_roots: continue
    audit = ROOT/mod_roots[mod_id]/'build/native-activation-audit/report.json'
    if not audit.is_file(): continue
    for entry in json.loads(audit.read_text())['mixins']:
        if entry['class'] not in classes_for_side: continue
        if entry.get('optionalMod') and entry['optionalMod'] not in installed: continue
        targets.update(t['owner'].replace('/', '.') for t in entry['targets'] if t.get('owner'))
if options.skip_activation or options.services_only or isolated_client or direction_matrix: targets.clear()
classes = work / 'driver-classes'
if isolated_client and classes.exists(): shutil.rmtree(classes)
classes.mkdir(exist_ok=True)
base = shlex.split((options.client_args or ROOT.parent / 'veil/build/native-sodium-verification/client.args').read_text())
index = base.index('-classpath')
cp = []
for raw in base[index + 1].split(os.pathsep):
    p = Path(raw)
    if not p.is_file():
        raise SystemExit(f'Non-jar classpath entry rejected: {p}')
    with zipfile.ZipFile(p) as jar:
        if 'fabric.mod.json' in jar.namelist():
            mod = json.loads(jar.read('fabric.mod.json'))['id']
            if mod in installed or mod == 'sodium' or 'acceptance' in mod or 'fixture' in mod:
                continue
    if p not in cp:
        cp.append(p)
cp += list(snapshots.values())
source = ROOT / 'tools/port-tests' / ('DirectionMatrixServer.java' if direction_matrix else ('PackagedSmokeServer.java' if options.side == 'server' else 'PackagedSmokeClient.java'))
sources = [source]
if external_driver: sources = [p.resolve() for p in options.driver_source]
if not isolated_client and not direction_matrix: sources.append(ROOT/'tools/port-tests/PackagedActivation.java')
if direction_matrix:
    sources += [ROOT/'tools/port-tests/DirectionMatrixSupport.java', ROOT/'tools/port-tests'/('Direction'+options.direction_checks.capitalize()+'Matrix.java')]
if options.side == 'client' and not isolated_client: sources.append(ROOT/'tools/port-tests/PackagedHandLightDepth.java')
if options.side == 'client' and 'aeronautics' in installed and not isolated_client and options.client_checks!='aeronautics-flight': sources.append(ROOT/'tools/port-tests/PackagedGustSync.java')
if options.side == 'client' and 'simulated' in installed and not isolated_client and options.client_checks!='aeronautics-flight': sources.append(ROOT/'tools/port-tests/PackagedAddonScenes.java')
if options.side == 'client' and options.client_checks=='endsea-visible': sources.append(ROOT/'tools/port-tests/PackagedEndSeaVisible.java')
if options.side == 'client' and options.client_checks=='aeronautics-flight': sources.append(ROOT/'tools/port-tests/PackagedAeronauticsFlight.java')
if item_resources or options.item_gui_only: sources.append(ROOT/'tools/port-tests/PackagedItemResources.java')
if (item_resources or reported_crash) and options.crash_checks != 'none': sources.append(ROOT/'tools/port-tests/PackagedCrashRegressions.java')
if ponder_block_entities: sources.append(ROOT/'tools/port-tests/PackagedPonderBlockEntities.java')
if borehead: sources.append(ROOT/'tools/port-tests/PackagedBorehead.java')
if warning_resources: sources.append(ROOT/'tools/port-tests/PackagedWarningResources.java')
if gameplay_audit:
    sources += [ROOT/'tools/port-tests/PackagedGameplayAudit.java',
                ROOT/'tools/port-tests/port/observation/GameplayItems.java',
                ROOT/'tools/port-tests/port/probe/GameplayItemResolverMixin.java']
    if options.audit_checks == 'ponder':
        sources += [ROOT/'tools/port-tests'/p for p in ['PackagedGameplayPonder.java',
            'port/observation/GameplayPonder.java','port/probe/GameplayPonderExtractMixin.java',
            'port/probe/GameplayPonderSubmitMixin.java','port/probe/GameplayPonderBlitMixin.java']]
    if options.audit_checks == 'tools': sources.append(ROOT/'tools/port-tests/PackagedGameplayTools.java')
if user_regressions:
    helper = {'diagram':'PackagedDiagramRegression.java', 'schematicannon':'PackagedSchematicannonRegression.java',
              'schematicannon-interaction':'PackagedSchematicannonInteraction.java', 'physical-walking':'PackagedPhysicalWalkingRegression.java', 'glue':'PackagedGlueVisibilityRegression.java', 'grass':'PackagedGrassStandingRegression.java', 'create-walking':'PackagedCreateWalkingRegression.java','create-micro-walking':'PackagedCreateMicroWalkingRegression.java', 'plot-interaction':'PackagedPlotInteractionRegression.java', 'assembler-lifecycle':'PackagedAssemblerLifecycleRegression.java', 'assembler-roundtrip':'PackagedAssemblerLifecycleRegression.java', 'airship-operation':'PackagedAirshipOperationRegression.java', 'airship-bootstrap':'PackagedAirshipBootstrapRegression.java', 'airship-preheat-launch':'PackagedAirshipPreheatLaunchRegression.java', 'airship-refuel':'PackagedAirshipRefuelRegression.java', 'airship-reload':'PackagedAirshipReloadRegression.java', 'airship-propulsion':'PackagedAirshipPropulsionRegression.java', 'plot-water':'PackagedPlotWaterRegression.java', 'airship-recovery':'PackagedAirshipRecoveryRegression.java', 'create-plot-retain':'PackagedCreatePlotRetainRegression.java', 'bundle-startup':'PackagedBundleStartupRegression.java','fabric-cleanup':'PackagedFabricCleanupRegression.java'}[options.regression_checks]
    sources.append(ROOT/'tools/port-tests'/helper)
    if options.regression_checks == 'fabric-cleanup':
        sources.append(ROOT/'tools/port-tests/PackagedBundleStartupRegression.java')
    if options.regression_checks in ('airship-propulsion', 'airship-recovery'):
        sources.append(ROOT/'tools/port-tests/PackagedAirshipReloadRegression.java')
    if options.regression_checks in ('airship-reload', 'airship-propulsion'):
        sources.append(ROOT/'tools/port-tests/PackagedAirshipRecoveryRegression.java')
if collision_probe:
    sources += [ROOT/'tools/port-tests/port/probe/RapierCollisionProbeMixin.java',
                ROOT/'tools/port-tests/port/observation/RapierCollisionProbeState.java']
if options.side == 'server' and 'simulated' in installed and not options.services_only and not direction_matrix: sources.append(ROOT/'tools/port-tests/PackagedDyeRecipe.java')
if options.side == 'server' and options.services_only and not direction_matrix: sources.append(ROOT/'tools/port-tests/PackagedNativeServices.java')
source_hashes = {}
frozen_sources = []
for ordinal, source in enumerate(sources):
    raw = source.read_bytes()
    source_hashes[str(source.relative_to(ROOT)) if source.is_relative_to(ROOT) else str(source)] = hashlib.sha256(raw).hexdigest()
    frozen = work/'driver-sources'/str(ordinal)/source.name
    frozen.parent.mkdir(parents=True, exist_ok=True)
    frozen.write_bytes(raw)
    frozen_sources.append(frozen)
compile_result = subprocess.run([str(JAVA/'javac'), '--release', '25', '-proc:none', '-sourcepath', '', '-cp', os.pathsep.join(map(str, [*cp, *nested_compile])), '-d', str(classes), *map(str, frozen_sources)], capture_output=True, text=True)
(work/'compile.log').write_text(compile_result.stdout+compile_result.stderr)
if compile_result.returncode:
    print(compile_result.stdout+compile_result.stderr)
    raise SystemExit(compile_result.returncode)
driver = work / 'packaged-smoke-driver.jar'
resource_hashes = {}
with zipfile.ZipFile(driver, 'w', zipfile.ZIP_DEFLATED) as jar:
    jar.writestr('packaged-activation-targets.txt', '\n'.join(sorted(targets))+'\n')
    if warning_resources:
        jar.writestr('assets/port_warning_test/models/component_tire.json', json.dumps({
            'parent':'minecraft:block/cube_all', 'textures':{'all':'minecraft:block/stone'}}))
        jar.writestr('assets/port_warning_test/models/component_mesh_tire.json', json.dumps({
            'parent':'minecraft:block/air', 'fabric:type':'create:mesh',
            'textures':{'wheel':'minecraft:block/stone','particle':'#wheel'},
            'mesh':[{'texture':'wheel','vertices':[
                [0,0,0,0,0,0,0,-1], [0,1,0,0,1,0,0,-1],
                [1,1,0,1,1,0,0,-1], [1,0,0,1,0,0,0,-1]]}]}))
        jar.writestr('assets/port_warning_test/models/bad_json.json', '{"elements": [')
        jar.writestr('assets/port_warning_test/models/bad_parent.json', json.dumps({'parent':'port_warning_test:absent_parent'}))
        jar.writestr('assets/port_warning_test/models/cyclic_parent.json', json.dumps({'parent':'port_warning_test:cyclic_parent'}))
    driver_metadata = {'schemaVersion':1, 'id':'packaged_smoke_driver', 'version':'1.0.0', 'environment':'*', 'name':'Packaged production smoke driver (test only)', 'entrypoints':({'main':['DirectionMatrixServer' if direction_matrix else 'PackagedSmokeServer']} if options.side == 'server' else {'fabric-client-gametest':[options.driver_entrypoint or 'PackagedSmokeClient']}), 'depends':{'fabricloader':'>=0.19.5', 'fabric-api':'*', 'minecraft':'26.3'}}
    if collision_probe:
        driver_metadata['mixins'] = ['packaged-smoke-probes.mixins.json']
        jar.writestr('packaged-smoke-probes.mixins.json', json.dumps({'required':True,'package':'port.probe','compatibilityLevel':'JAVA_25','mixins':['RapierCollisionProbeMixin'],'injectors':{'defaultRequire':1}}))
    if gameplay_audit:
        driver_metadata.setdefault('mixins', []).append('packaged-gameplay-observation.mixins.json')
        probes = ['GameplayItemResolverMixin']
        if options.audit_checks == 'ponder': probes += ['GameplayPonderExtractMixin','GameplayPonderSubmitMixin','GameplayPonderBlitMixin']
        jar.writestr('packaged-gameplay-observation.mixins.json', json.dumps({'required':True,'package':'port.probe','compatibilityLevel':'JAVA_25','client':probes,'injectors':{'defaultRequire':1}}))
    jar.writestr('fabric.mod.json', json.dumps(driver_metadata))
    if item_resources:
        jar.writestr('item-resource-expectations.json', (ROOT/'build/porting/item-localization/source-key-audit.json').read_bytes())
    if ponder_block_entities and not options.ponder_owners_only:
        jar.writestr('ponder-engine-expectations.json', (ROOT/'build/porting/ponder-palettes/engine-be-regression-expectations.json').read_bytes())
    for p in classes.rglob('*.class'):
        name = str(p.relative_to(classes))
        if name in production_classes: raise SystemExit('Test driver would replace a production class: '+name)
        jar.write(p, name)
    for folder in options.driver_resources:
        if not folder.is_dir(): raise SystemExit('Missing test resource directory: '+str(folder))
        for p in sorted(folder.resolve().rglob('*')):
            if not p.is_file(): continue
            name = str(p.relative_to(folder.resolve()))
            if name in production_assets or name == 'fabric.mod.json' or name.endswith('.class'):
                raise SystemExit('Test driver would replace production content: '+name)
            raw = p.read_bytes()
            resource_hashes[str(p)] = hashlib.sha256(raw).hexdigest()
            jar.writestr(name, raw)
cp.append(driver)
run = work / 'run'
run.mkdir(exist_ok=True)
if options.resume_world:
    original_world = options.resume_world.resolve()
    copied_world = run/'saves'/original_world.name
    marker = run/'resumed-world-source.json'
    if not marker.exists():
        if copied_world.exists() and any(copied_world.iterdir()):
            raise SystemExit('Refusing to overwrite an untracked saved world: '+str(copied_world))
        shutil.copytree(original_world, copied_world, dirs_exist_ok=True)
        marker.write_text(json.dumps({'source':str(original_world), 'originalLevelDatSha256':hashlib.sha256((original_world/'level.dat').read_bytes()).hexdigest()},indent=2)+'\n')
    else:
        if json.loads(marker.read_text())['source'] != str(original_world):
            raise SystemExit('Saved-world continuation source changed')
report = work / 'runtime.json'
report.unlink(missing_ok=True)
args = [a for a in base[:index] if not a.startswith(('-Dveil.', '-Dfabric.client.gametest'))]
if options.side == 'server':
    args = [a for a in args if a != '-XstartOnFirstThread']
    (run/'eula.txt').write_text('eula=true\n')
    properties = 'online-mode=false\nserver-port=0\nview-distance=2\nsimulation-distance=2\nlevel-seed=1\nspawn-protection=0\n'
    if direction_matrix:
        properties += 'level-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\n'
        properties += 'max-tick-time=-1\npause-when-empty-seconds=0\ngenerate-structures=false\n'
    (run/'server.properties').write_text(properties)
    tail = ['net.fabricmc.loader.impl.launch.knot.KnotServer', '--nogui']
    if options.services_only: args.append('-Dport.smoke.servicesOnly=true')
    if options.services_only: args.append('-Dport.smoke.servicesChecks='+options.services_checks)
    elif direction_matrix:
        args += ['-Dport.matrix.group='+options.direction_checks, '-Dport.matrix.phase='+options.direction_phase, '-Dport.matrix.tickRate='+str(options.direction_tick_rate)]
    elif 'simulated' in installed: args.append('-Dport.smoke.dyeRecipe=true')
else:
    args += ['-Dfabric.client.gametest=true', '-Dfabric.client.gametest.modid=packaged_smoke_driver', '-Dfabric.client.gametest.testModResourcesPath='+str(ROOT/'tools/port-tests')]
    if external_driver:
        args += ['-D'+value for value in options.driver_property]
        if options.resume_world: args.append('-Dport.smoke.resumeWorld='+str(copied_world))
    if options.hand_depth and not isolated_client and options.client_checks!='aeronautics-flight': args.append('-Dport.smoke.handDepth=true')
    if 'aeronautics' in installed and not isolated_client and options.client_checks=='all': args.append('-Dport.smoke.gustSync=true')
    if 'simulated' in installed and not isolated_client and options.client_checks not in ('endsea-visible','aeronautics-flight'): args.append('-Dport.smoke.addonScenes=true')
    if item_resources: args.append('-Dport.smoke.itemResources=true')
    if gameplay_audit: args += ['-Dport.smoke.gameplayAudit=true', '-Dport.smoke.auditChecks='+options.audit_checks]
    if gameplay_audit and options.audit_item: args.append('-Dport.smoke.auditItem='+options.audit_item)
    if gameplay_audit and options.audit_case: args.append('-Dport.smoke.auditCase='+options.audit_case)
    if warning_resources: args += ['-Dport.smoke.warningResources=true', '-Dport.smoke.warningChecks='+options.warning_checks]
    if user_regressions:
        args += ['-Dport.smoke.userRegression='+options.regression_checks,
                 '-Dport.regression.schematic='+str(ROOT/'build/porting/airshipbase/airshipbase.original.nbt')]
        if options.physical_accepted_static:
            args.append('-Dport.smoke.physicalWalkingAcceptedStatic='+str(options.physical_accepted_static.resolve()))
        if options.physical_rotation: args.append('-Dport.smoke.physicalRotation=true')
        if options.create_support_probes_only: args.append('-Dport.smoke.createSupportProbesOnly=true')
        if options.airship_operation_accepted: args.append('-Dport.smoke.airshipAcceptedOperation='+str(options.airship_operation_accepted.resolve()))
        if options.airship_controls_accepted: args.append('-Dport.smoke.airshipAcceptedControls='+str(options.airship_controls_accepted.resolve()))
        args.append('-Dport.regression.cannon.mode='+options.cannon_mode)
        if options.resume_world: args.append('-Dport.smoke.resumeWorld='+str(options.resume_world.resolve()))
    if reported_crash: args.append('-Dport.smoke.reportedCrash=true')
    if ponder_block_entities: args.append('-Dport.smoke.ponderBlockEntities=true')
    if borehead: args.append('-Dport.smoke.borehead=true')
    if options.ponder_owners_only: args.append('-Dport.smoke.ponderOwnersOnly=true')
    if options.item_gui_only: args.append('-Dport.smoke.itemGuiOnly=true')
    if isolated_client: args.append('-Dport.smoke.crashChecks='+options.crash_checks)
    if options.client_checks=='ponder': args.append('-Dport.smoke.ponderOnly=true')
    if options.client_checks=='endsea-visible': args.append('-Dport.smoke.endseaVisible=true')
    if options.client_checks=='aeronautics-flight':
        args.append('-Dport.smoke.aeronauticsFlight=true')
        if options.keep_open: args.append('-Dport.smoke.keepOpen=true')
    tail = base[index+2:]
    tail[tail.index('--gameDir')+1] = str(run)
    if options.client_checks=='aeronautics-flight' or isolated_client:
        for option,value in [('--width','1280'),('--height','720')]:
            if option in tail: tail[tail.index(option)+1]=value
            else: tail.extend([option,value])
args += ['-Dport.smoke.report='+str(report), '-Dport.smoke.activationReport='+str(work/'activation.json'), '-classpath', os.pathsep.join(map(str,cp)), *tail]
argfile = work / 'launch.args'
argfile.write_text('\n'.join('"'+a.replace('\\','\\\\').replace('"','\\"')+'"' for a in args)+'\n')
manifest = {'scope':'Actual packaged production activation/world and selected affected integration scenes; does not certify every gameplay feature.', 'side':options.side, 'mods':mod_hashes, 'frozenMods':{str(p.relative_to(ROOT)):mod_hashes[str(original.relative_to(ROOT)) if original.is_relative_to(ROOT) else str(original)] for original,p in snapshots.items()}, 'driver':source_hashes, 'driverClasses':{str(p.relative_to(classes)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(classes.rglob('*.class'))}, 'scriptSha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
manifest['activationTargets'] = sorted(targets)
manifest['driverJarSha256'] = hashlib.sha256(driver.read_bytes()).hexdigest()
manifest['selectedChecks'] = {'activation':not (options.skip_activation or options.services_only or isolated_client or direction_matrix), 'servicesOnly':options.services_only, 'handDepth':options.hand_depth, 'gustSync':options.side=='client' and 'aeronautics' in installed, 'addonScenes':options.side=='client' and 'simulated' in installed}
manifest['selectedChecks'].update({'clientSelection':options.client_checks, 'servicesSelection':options.services_checks})
if external_driver:
    manifest['externalDriver'] = {'entrypoint':options.driver_entrypoint, 'properties':options.driver_property,
        'resources':resource_hashes}
    manifest['selectedChecks']['clientSelection'] = 'external'
if options.resume_world:
    manifest['resumedWorld'] = str(options.resume_world.resolve())
    manifest['resumedWorldInput'] = json.loads((run/'resumed-world-source.json').read_text())
if gameplay_audit:
    manifest['selectedChecks'].update({'gameplaySelection':options.audit_checks, 'itemFilter':options.audit_item, 'scenarioFilter':options.audit_case})
manifest['selectedChecks']['gustSync'] &= not isolated_client and options.client_checks=='all'
manifest['selectedChecks']['addonScenes'] &= not isolated_client and options.client_checks not in ('endsea-visible','aeronautics-flight')
manifest['selectedChecks']['endseaVisible'] = options.side=='client' and options.client_checks=='endsea-visible'
manifest['selectedChecks']['aeronauticsFlight'] = options.side=='client' and options.client_checks=='aeronautics-flight'
manifest['selectedChecks']['handDepth'] &= not isolated_client and options.client_checks!='aeronautics-flight'
manifest['selectedChecks']['itemResources'] = item_resources
manifest['selectedChecks']['warningResources'] = warning_resources
if warning_resources: manifest['selectedChecks']['warningResourceSelection'] = options.warning_checks
if user_regressions:
    manifest['selectedChecks']['userRegression'] = options.regression_checks
    manifest['selectedChecks']['physicalRotation'] = options.physical_rotation
    manifest['selectedChecks']['createSupportProbesOnly'] = options.create_support_probes_only
    if options.airship_operation_accepted:
        manifest['acceptedAirshipOperation'] = {'path':str(options.airship_operation_accepted.resolve()), 'sha256':hashlib.sha256(options.airship_operation_accepted.read_bytes()).hexdigest()}
    if options.airship_controls_accepted:
        manifest['acceptedAirshipControls'] = {'path':str(options.airship_controls_accepted.resolve()), 'sha256':hashlib.sha256(options.airship_controls_accepted.read_bytes()).hexdigest()}
    manifest['selectedChecks']['cannonMode'] = options.cannon_mode
    if options.resume_world: manifest['resumedWorld'] = str(options.resume_world.resolve())
    schematic_input = ROOT/'build/porting/airshipbase/airshipbase.original.nbt'
    if schematic_input.exists(): manifest['schematicInputSha256'] = hashlib.sha256(schematic_input.read_bytes()).hexdigest()
    if options.physical_accepted_static:
        manifest['acceptedStaticWalking'] = {'path':str(options.physical_accepted_static.resolve()), 'sha256':hashlib.sha256(options.physical_accepted_static.read_bytes()).hexdigest()}
manifest['selectedChecks']['itemGuiOnly'] = options.item_gui_only
manifest['selectedChecks']['reportedCrash'] = (item_resources or reported_crash) and options.crash_checks != 'none'
manifest['selectedChecks']['crashSelection'] = options.crash_checks if item_resources or reported_crash else 'none'
manifest['selectedChecks']['ponderBlockEntities'] = ponder_block_entities
manifest['selectedChecks']['borehead'] = borehead
manifest['selectedChecks']['ponderOwnersOnly'] = options.ponder_owners_only
if ponder_block_entities and not options.ponder_owners_only: manifest['ponderEngineExpectationsSha256'] = hashlib.sha256((ROOT/'build/porting/ponder-palettes/engine-be-regression-expectations.json').read_bytes()).hexdigest()
if item_resources: manifest['itemNameExpectationsSha256'] = hashlib.sha256((ROOT/'build/porting/item-localization/source-key-audit.json').read_bytes()).hexdigest()
manifest['keepOpen'] = options.keep_open
if direction_matrix: manifest['selectedChecks']['directionMatrix'] = {'group':options.direction_checks, 'phase':options.direction_phase, 'tickRate':options.direction_tick_rate}
(work/'inputs.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Launching complete production jars:', ', '.join(p.name for p in mods), flush=True)
with (work/'runtime.log').open('w') as log:
    try:
        result = subprocess.run([str(JAVA/'java'),'@'+str(argfile)], cwd=run, stdout=log, stderr=subprocess.STDOUT, timeout=None if options.keep_open else (3600 if direction_matrix else (900 if external_driver or gameplay_audit or user_regressions and options.cannon_mode == 'full' else 300)))
    except subprocess.TimeoutExpired:
        raise SystemExit('Production startup timed out; inspect '+str(work/'runtime.log'))
if result.returncode or not report.is_file():
    print((work/'runtime.log').read_text()[-6500:])
    raise SystemExit(result.returncode or 1)
data = json.loads(report.read_text())
print(json.dumps(data,ensure_ascii=False))
if data.get('status') != 'passed':
    raise SystemExit(1)
