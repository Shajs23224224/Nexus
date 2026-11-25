#!/usr/bin/env python3
"""Apply to an extracted NEXUS 0.5 source tree. Never modifies an unmatched revision."""
import sys
from pathlib import Path

suffix = Path("app/src/main/java/co/nexus/lab/live/LiveService.java")
root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
path = next((p for p in (root / suffix, root / "NexusAI" / suffix) if p.is_file()), None)
if path is None:
    sys.exit("Se necesita el código de NEXUS 0.5.")
source = path.read_text(encoding="utf-8")
edits = [
    ("private long deadline,nextTap;", "private long deadline,nextTap,unavailableSince;"),
    ("    private final Runnable guard=", """    private void checkWindow(){
        if(!entered||game==null)return;
        if(foreground()){unavailableSince=0;return;}
        // Window metadata can briefly be empty as the gesture picker is removed.
        // Stop all action immediately; wait for a stable departure before removing the bar.
        if(unavailableSince==0)unavailableSince=SystemClock.elapsedRealtime();
        if(mode==1&&(tracking||reading))matchIdle("Ventana en transición");
        if(SystemClock.elapsedRealtime()-unavailableSince>=2200)end("El juego salió de primer plano.");
    }
    private final Runnable guard="""),
    ('if(!foreground()){if(entered){end("El juego salió de primer plano.");return;}}',
     'if(!foreground()){if(entered){checkWindow();if(game==null)return;}}'),
    ('else if(!entered){entered=true;', 'else if(!entered){unavailableSince=0;entered=true;'),
    ('        loop.postDelayed(this,320);','        if(entered&&foreground())unavailableSince=0;\n        loop.postDelayed(this,320);'),
    (' && !foreground())end("Se cerró la sesión al cambiar de ventana.");', ')checkWindow();'),
    ('game=null;pending=null;', 'game=null;unavailableSince=0;pending=null;'),
    ('if(!foreground()||!sameDisplay()||SystemClock.elapsedRealtime()>deadline){end("Sesión finalizada.");return false;}',
     '''if(!foreground()){
            if(mode==1){checkWindow();return false;}
            end("Sesión finalizada.");return false;
        }
        if(!sameDisplay()||SystemClock.elapsedRealtime()>deadline){end("Sesión finalizada.");return false;}'''),
    ('        pending=features;reading=true;bar.setVisibility(View.GONE);picker=new FrameLayout(this);',
     '        pending=features;reading=true;picker=new FrameLayout(this);'),
    ('        wm.addView(picker,params(-1,-1));\n    }\n    private void perform',
     '''        // Keep the original panel above the full-screen gesture picker.
        // Detener remains available while recording and while Android dispatches the gesture.
        try{
            wm.addView(picker,params(-1,-1));
            remove(bar);
            wm.addView(bar,params(-2,-2));
        }catch(RuntimeException error){end("No se pudo mostrar el panel de enseñanza.");}
    }
    private void perform'''),
    ('        if(!valid(id))return;\n        GestureDescription.Builder b=new GestureDescription.Builder();stroke(b,x,y,u,v,ms);',
     '''        if(!valid(id))return;
        if(mode==1)loop.postDelayed(()->{
            if(game!=null && generation==id && tracking)matchIdle("Tiempo agotado · reintenta");
        },ms+3000L);
        GestureDescription.Builder b=new GestureDescription.Builder();stroke(b,x,y,u,v,ms);'''),
]
for old, new in edits:
    if source.count(old) != 1:
        sys.exit("Revisión incompatible. Archivo original intacto.")
    source = source.replace(old, new, 1)
temp = path.with_suffix(".java.tmp")
with temp.open("x", encoding="utf-8") as output:
    output.write(source)
temp.replace(path)
print("Parche de enseñanza aplicado a", path)
