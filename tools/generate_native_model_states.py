#!/usr/bin/env python3
"""Regenerate/check the three migrated model-state definitions without rebuilding Create."""
import argparse,collections,hashlib,json,os,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--write',action='store_true');args=parser.parse_args()
java=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
work=root/'build/porting/native-model-states';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
manifest=json.loads((root/'build/porting/suite-diagnostic/inputs.json').read_text())
sources=[root/p for p in [
'simulated/fabric/src/main/java/dev/simulated_team/simulated/fabric/data/NativeBlockStateJson.java',
'simulated/common/src/main/java/dev/simulated_team/simulated/content/blocks/steering_wheel/SteeringWheelGenerator.java',
'simulated/common/src/main/java/dev/simulated_team/simulated/content/blocks/directional_gearshift/DirectionalGearshiftGenerator.java',
'simulated/common/src/main/java/dev/simulated_team/simulated/content/blocks/redstone/redstone_accumulator/RedstoneAccumulatorBlockStateGen.java',
 'tools/java/GenerateNativeModelStates.java']]
classpath=os.pathsep.join(p for p in manifest['classpath'] if '/gson/' in p)
result=subprocess.run([str(java/'javac'),'--release','25','-proc:none','-sourcepath','','-classpath',classpath,'-d',str(classes),*map(str,sources)],text=True,capture_output=True)
(work/'compile.log').write_text(result.stdout+result.stderr)
if result.returncode:raise SystemExit(result.stderr)
output=work/'generated';subprocess.run([str(java/'java'),'-cp',str(classes)+os.pathsep+classpath,'GenerateNativeModelStates',str(output)],check=True)
assets=root/'simulated/common/src/generated/resources/assets/simulated/blockstates'
counts={}
for generated in sorted(output.glob('*.json')):
 original=assets/generated.name;golden=json.loads(original.read_text());actual=json.loads(generated.read_text())
 if 'multipart' in golden:
  normalized=lambda m:collections.Counter(json.dumps(part,sort_keys=True) for part in m['multipart'])
  if normalized(actual)!=normalized(golden):raise SystemExit('Native definition differs from shipped original '+original.name)
  counts[generated.stem]=len(actual['multipart'])
 else:
  if actual!=golden:raise SystemExit('Native definition differs from shipped original '+original.name)
  counts[generated.stem]=len(actual['variants'])
 if args.write:original.write_text(generated.read_text())
inputs={'result':'PASS','scope':'3 native resource generators; equality to all shipped original variants/multipart entries including duplicate multiplicities; no model bake/world claim','entries':counts,'inputs':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [*sources,Path(__file__).resolve(),*[assets/n.name for n in output.glob('*.json')]]}}
(work/'inputs.json').write_text(json.dumps(inputs,indent=2)+'\n')
print('Native model-state generation PASS:',json.dumps(counts),'write='+str(args.write))
