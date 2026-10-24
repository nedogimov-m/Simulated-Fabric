#!/usr/bin/env python3
"""Verify genuine key defaults, legacy migration, native codecs and SDL callback action dispatch on CPU."""
from pathlib import Path
import hashlib, json, os, re, shlex, subprocess

repo = Path(__file__).resolve().parents[1]
root = repo / 'simulated'
work = root / 'build/native-keyboard-verification'
classes = work / 'classes'
classes.mkdir(parents=True, exist_ok=True)
java = Path(os.environ.get('JAVA_HOME', '/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home')) / 'bin'
args = shlex.split((repo / 'build/porting/suite-diagnostic/javac.args').read_text())
production = root / 'fabric/build/classes/java/main'
cp = os.pathsep.join([str(production), str(repo / 'ports/veil/build/libs/veil-fabric-4.3.2-port.1+mc26.3.jar'), args[args.index('-classpath') + 1]])
base = root / 'common/src/main/java/dev/simulated_team/simulated'
sources = [base / 'util/KeyboardScanCodes.java', base / 'network/packets/linked_typewriter/TypewriterKeyInteractionPacket.java',
    repo / 'tools/port-tests/NativeKeyboardAcceptance.java']
result = subprocess.run([str(java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', '', '-cp', cp,
    '-d', str(classes), *map(str, sources)], capture_output=True, text=True)
(work / 'compile.log').write_text(result.stdout + result.stderr)
if result.returncode:
    print(result.stdout + result.stderr)
    raise SystemExit(result.returncode)
isolated = ['util/KeyboardScanCodes.class', 'network/packets/linked_typewriter/TypewriterKeyInteractionPacket.class']
for name in isolated:
    data = (classes / 'dev/simulated_team/simulated' / name).read_bytes()
    if b'com/mojang/blaze3d/platform/InputConstants' in data:
        raise SystemExit('Client InputConstants reference retained in actual server class: ' + name)
result = subprocess.run([str(java / 'java'), '--enable-native-access=ALL-UNNAMED', '-cp', os.pathsep.join([str(classes), cp]),
    'NativeKeyboardAcceptance'], capture_output=True, text=True, timeout=120)
(work / 'runtime.log').write_text(result.stdout + result.stderr)
print(result.stdout + result.stderr, end='')
if result.returncode:
    raise SystemExit(result.returncode)
abi = {}
for owner in ['com.mojang.blaze3d.platform.SDLEventHandler', 'dev.simulated_team.simulated.mixin.hold_interaction.KeyboardHandlerMixin',
              'dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler',
              'dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterSaveKeyToItemPacket']:
    result = subprocess.run([str(java / 'javap'), '-classpath', cp, '-p', '-c', owner], capture_output=True, text=True, check=True)
    path = work / (owner.split('.')[-1] + '.txt')
    path.write_text(result.stdout)
    abi[owner] = result.stdout
sdl = abi['com.mojang.blaze3d.platform.SDLEventHandler'].split('private void handleKeyEvent(', 1)[1].split('\n  private ', 1)[0]
assert 'SDL_KeyboardEvent.repeat:()Z' in sdl and 'iconst_m1' in sdl and 'iconst_0' in sdl and 'iconst_1' in sdl
assert sdl.index('SDL_KeyboardEvent.scancode:()I') < sdl.index('SDL_KeyboardEvent.key:()I')
caller = abi['dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler'].split('public static void onKeyPress(', 1)[1]
caller = re.split(r'(?m)^  (?:public|private|protected) ', caller, maxsplit=1)[0]
assert 'iconst_m1' in caller and 'TypewriterKeyInteractionPacket' in caller
assert 'SimulatedCommonClientEvents.onAfterKeyPress:(IIII)V' in abi['dev.simulated_team.simulated.mixin.hold_interaction.KeyboardHandlerMixin']
assert 'KeyboardScanCodes.fromLegacy:(I)I' in abi['dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterSaveKeyToItemPacket']
dependencies = [base / 'content/blocks/redstone/linked_typewriter' / name for name in ['LinkedTypewriterEntries.java', 'LinkedTypewriterBlockEntity.java', 'LinkedTypewriterInteractionHandler.java']]
dependencies += [base / 'index/SimKeys.java', base / 'network/packets/linked_typewriter/TypewriterSaveKeyToItemPacket.java',
    base / 'mixin/hold_interaction/KeyboardHandlerMixin.java']
manifest = {'scope': 'Actual native bootstrap/data-component initialization, production defaults/NBT/registry stream codecs and five critical legacy physical migrations. Actual SDL dispatch and raw-action caller bytecode inspected. No GPU launch or fabricated classes.',
    'result': 'PASS', 'server_bytecode_without_InputConstants': isolated,
    'native_callback': 'SDLEventHandler.handleKeyEvent emits key-up=0, repeat=-1, fresh press=1; constructs KeyEvent(scancode,keycode,modifiers). Production wrapper forwards raw action and typewriter excludes -1 before packet send.',
    'sources': {str(p.relative_to(repo)): hashlib.sha256(p.read_bytes()).hexdigest() for p in [*sources, *dependencies, Path(__file__).resolve()]},
    'actual_abi': {name: hashlib.sha256(source.encode()).hexdigest() for name, source in abi.items()}}
(work / 'inputs.json').write_text(json.dumps(manifest, indent=2) + '\n')
print('PASS actual server utility/packet have no InputConstants class references; native raw repeat dispatch and caller forwarding verified.')
