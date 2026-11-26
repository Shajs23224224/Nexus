"""Extend the archived original NEXUS Android project, not the root Research app."""
from pathlib import Path
import shutil, sys, subprocess
import xml.etree.ElementTree as ET

repo=Path(__file__).resolve().parent.parent
project=Path(sys.argv[1]).resolve()
app=project/'app'
java=app/'src/main/java'
source=java/'co/nexus/lab/FieldActivity.java'
manifest=app/'src/main/AndroidManifest.xml'
assert source.exists() and (java/'co/nexus/lab/live/LiveService.java').exists()
assert (java/'co/nexus/video/Analysis.java').exists()
# Pin the tested research implementation and copy only the local cognitive modules.
rev='9af471a8743e6568c5552f925f87001b12248de4'
files=['CognitiveCore','CognitiveArena','CognitiveActivity','SparseBrain','SparseActivity','StrategyLab','VideoStudy','VideoAudit','VideoAuditService']
dest=java/'co/nexus/research'
dest.mkdir(parents=True,exist_ok=True)
for name in files:
    path=f'app/src/main/java/co/nexus/research/{name}.java'
    (dest/f'{name}.java').write_bytes(subprocess.check_output(['git','show',f'{rev}:{path}'],cwd=repo))
for name in ['ShooterWorld','ShooterScene','ShooterActivity']:
    shutil.copyfile(repo/'original/shooter'/f'{name}.java',dest/f'{name}.java')
# Retain the original launcher and insert one native menu entry in its existing style.
s=source.read_text(encoding='utf-8')
anchor='        select.setOnItemSelectedListener('
assert s.count(anchor)==1
s=s.replace(anchor,'        root.addView(label("Inteligencia cognitiva",22,INK));\n        button(row(),"Shooter 3D · IA autónoma",()->startActivity(new Intent(this,co.nexus.research.ShooterActivity.class)),true);\n        button(row(),"Revisión de video y estrategias",()->startActivity(new Intent(this,co.nexus.research.SparseActivity.class)),false);\n'+anchor)
s=s.replace('button(row(),"Laboratorio 2D y modelos anteriores",()->startActivity(new Intent(this,NexusActivity.class)),false);','button(row(),"Archivo de modelos anteriores",()->startActivity(new Intent(this,NexusActivity.class)),false);')
source.write_text(s,encoding='utf-8')
# The original application ID, icon, launcher, live setup, resources and video screens stay intact.
ns='http://schemas.android.com/apk/res/android'
ET.register_namespace('android',ns)
def at(name): return '{'+ns+'}'+name
tree=ET.parse(manifest)
root=tree.getroot()
root.set(at('versionCode'),'21')
root.set(at('versionName'),'0.18.0')
application=root.find('application')
assert application is not None
for name in ['co.nexus.research.CognitiveActivity','co.nexus.research.SparseActivity']:
    ET.SubElement(application,'activity',{at('name'):name,at('exported'):'false'})
ET.SubElement(application,'activity',{at('name'):'co.nexus.research.ShooterActivity',at('exported'):'false',at('screenOrientation'):'landscape'})
ET.SubElement(application,'service',{at('name'):'co.nexus.research.VideoAuditService',at('exported'):'false',at('foregroundServiceType'):'dataSync'})
for name in ['android.permission.FOREGROUND_SERVICE_DATA_SYNC','android.permission.POST_NOTIFICATIONS']:
    if not any(p.get(at('name'))==name for p in root.findall('uses-permission')):
        root.insert(0,ET.Element('uses-permission',{at('name'):name}))
tree.write(manifest,encoding='utf-8',xml_declaration=True)
gradle=app/'build.gradle'
g=gradle.read_text()
assert "applicationId 'co.nexus.lab.design'" in g
g=g.replace('versionCode 15','versionCode 21').replace("versionName '0.12.0'","versionName '0.18.0'")
g+='\ndependencies { implementation "com.google.mlkit:text-recognition:16.0.1"; implementation "com.google.mlkit:image-labeling:17.0.9" }\n'
gradle.write_text(g)
props=project/'gradle.properties'
p=props.read_text() if props.exists() else ''
if 'android.useAndroidX' not in p:p+='\nandroid.useAndroidX=true\n'
props.write_text(p)
print('Original NEXUS source extended; launcher FieldActivity and application ID preserved.')
