#!/usr/bin/env python3
"""Prepare a separate 0.5.1 tree from the distributed 0.5 source archive."""
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

here = Path(__file__).resolve().parent
archive = Path(sys.argv[1]).resolve()
dest = Path(sys.argv[2]).resolve()
if dest.exists():
    sys.exit("El destino debe ser una carpeta nueva.")
dest.parent.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(dir=dest.parent) as tmp:
    root = Path(tmp)
    with zipfile.ZipFile(archive) as z:
        for item in z.infolist():
            if not (root / item.filename).resolve().is_relative_to(root):
                sys.exit("Ruta de archivo inválida.")
        z.extractall(root)
    project = root / "NexusAI"
    live = project / "app/src/main/java/co/nexus/lab/live"
    subprocess.run([sys.executable, str(here / "nexus05_panel.py"), str(project)], check=True)
    shutil.copyfile(here / "0.5.1/MatchBook.java", live / "MatchBook.java")
    service = live / "LiveService.java"
    old = "generation++;unknown=repeat=0;previous=null;tracking=true;"
    s = service.read_text()
    if s.count(old) != 1:
        sys.exit("Versión de servicio incompatible.")
    service.write_text(s.replace(old, "book.resetDecision();" + old))
    for name, a, b in [
        ("app/build.gradle", "versionCode 5", "versionCode 6"),
        ("app/build.gradle", "0.5.0", "0.5.1"),
        ("app/src/main/AndroidManifest.xml", 'versionCode="5"', 'versionCode="6"'),
        ("app/src/main/AndroidManifest.xml", 'versionName="0.5.0"', 'versionName="0.5.1"'),
    ]:
        path = project / name
        s = path.read_text()
        if s.count(a) != 1:
            sys.exit("Metadatos de versión incompatibles.")
        path.write_text(s.replace(a, b))
    # The existing single-frame test must supply repeated observations.
    test = project / "tests/MatchBookTest.java"
    s = test.read_text()
    old = "MatchBook.Example e=b.choose(phase,scene(i));"
    if s.count(old) != 1:
        sys.exit("Prueba de regresión incompatible.")
    test.write_text(s.replace(old, "if(b.choose(phase,scene(i))!=null)throw new AssertionError(\"Un solo frame\");\n            if(i==3)b.choose(phase,scene(i));\n            " + old))
    # This test is host-only and doesn't require a connected phone.
    classes = root / "classes"
    classes.mkdir()
    subprocess.run(["javac", "-d", str(classes), str(live / "MatchBook.java"), str(test)], check=True)
    subprocess.run(["java", "-cp", str(classes), "co.nexus.lab.live.MatchBookTest"], check=True)
    project.rename(dest)
print(dest)
