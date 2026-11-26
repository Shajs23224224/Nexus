"""Include the video review screen and its service in the existing NEXUS Android project."""
import argparse
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("project", type=Path)
main = parser.parse_args().project / "app/src/main"
android = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", android)
key = lambda name: "{%s}%s" % (android, name)
manifest = main / "AndroidManifest.xml"
tree = ET.parse(manifest)
root = tree.getroot()
app = root.find("application")
activity = main / "java/co/nexus/lab/FieldActivity.java"
source = activity.read_text()
anchor = 'button(row(),"Prueba de controles en Free Fire"'
entry = '''if(android.os.Build.VERSION.SDK_INT>=28)
            button(row(),"Análisis de video",()->startActivity(new Intent(this,co.nexus.video.Review.class)),false);
        '''
if entry not in source:
    if source.count(anchor) != 1:
        raise ValueError("No se encontró el menú de NEXUS AI")
    source = source.replace(anchor, entry + anchor)
permission = "android.permission.FOREGROUND_SERVICE"
if not any(e.get(key("name")) == permission for e in root.findall("uses-permission")):
    root.insert(0, ET.Element("uses-permission", {key("name"): permission}))
for kind, cls in [("activity", "co.nexus.video.Review"), ("service", "co.nexus.video.Analysis")]:
    if not any(e.get(key("name")) == cls for e in app.findall(kind)):
        ET.SubElement(app, kind, {key("name"): cls, key("exported"): "false"})
dest = main / "java/co/nexus/video"
dest.mkdir(exist_ok=True, parents=True)
sources = Path(__file__).resolve().parents[1] / "video"
for name in ("Analysis.java", "Region.java", "Review.java"):
    shutil.copyfile(sources / name, dest / name)
tree.write(manifest, encoding="utf-8", xml_declaration=True)
activity.write_text(source)
