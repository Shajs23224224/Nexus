"""Apply supply search to the unified NEXUS 0.12 source tree."""
from pathlib import Path
import sys, shutil

root=Path(sys.argv[1])/'app'
live=root/'src/main/java/co/nexus/lab/live'
path=live/'LiveService.java'
s=path.read_text()
def replace(a,b):
    global s
    if s.count(a)!=1: raise ValueError("Incompatible source: "+a)
    s=s.replace(a,b)
replace('    private int textBudget;', '''    private SupplyPlan supplies;
    private SupplyPlan.Frame items;
    private SupplyPlan.Kind pendingItem=SupplyPlan.Kind.NONE;
    private byte[] pendingAppearance;
    private long verifyUntil, retrySupply;
    private int searchSteps;
    private int textBudget;
    private float[] findSupply(SupplyPlan.Kind goal,android.content.SharedPreferences hud){
        float[] named=items.locations[goal.ordinal()];
        if(named!=null)return named;
        if(goal==SupplyPlan.Kind.UNKNOWN || !hud.contains("lootX"))return null;
        float x=hud.getFloat("lootX",0), y=hud.getFloat("lootY",0);
        byte[] patch=VisualCues.patch(rgb,x,y);
        SupplyPlan.Kind only=SupplyPlan.Kind.NONE;
        for(SupplyPlan.Kind kind:SupplyPlan.Kind.values()){
            String encoded=hud.getString("supplyLook"+kind.name(),"");
            if(encoded.isEmpty())continue;
            byte[] ref=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
            if(!VisualCues.matches(ref,patch))continue;
            if(only!=SupplyPlan.Kind.NONE)return null;
            only=kind;
        }
        return only==goal?new float[]{x,y}:null;
    }''')
replace('tactics=new Tactics();', '''supplies=new SupplyPlan();pendingItem=SupplyPlan.Kind.NONE;searchSteps=0;retrySupply=0;tactics=new Tactics();''')
replace('GameText signals=new GameText();','GameText signals=new GameText();items=new SupplyPlan.Frame();')
replace('signals.read(node.getText());signals.read(node.getContentDescription());','''android.graphics.Rect rect=new android.graphics.Rect();
            node.getBoundsInScreen(rect);
            float x=rect.exactCenterX()/width,y=rect.exactCenterY()/height;
            for(CharSequence value:new CharSequence[]{node.getText(),node.getContentDescription()}){
                signals.read(value);
                if(value!=null)items.observe(value.toString(),x,y);
            }''')
replace('GameText labels=readGameText();','''GameText labels=readGameText();
        supplies.update(items);
        if(pendingItem!=SupplyPlan.Kind.NONE){
            boolean confirmed=pendingItem==SupplyPlan.Kind.KIT?items.kits>0 && !supplies.needKits:
                items.reserve>0 && pendingItem==supplies.equipped && !supplies.needAmmo;
            if(confirmed && now<=verifyUntil && pendingAppearance!=null)
                hud.edit().putString("supplyLook"+pendingItem.name(),
                    android.util.Base64.encodeToString(pendingAppearance,android.util.Base64.NO_WRAP)).apply();
            if(confirmed || now>=verifyUntil)pendingItem=SupplyPlan.Kind.NONE;
        }''')
replace('!labels.emptyMagazine && !labels.noReserve','!labels.emptyMagazine && !supplies.needAmmo')
replace('&& !labels.noKit','&& !supplies.needKits')
replace('labels.emptyMagazine && !labels.noReserve','labels.emptyMagazine && !supplies.needAmmo')
replace('        if(lootFrames>=2){','''        SupplyPlan.Kind goal=supplies.goal(hp);
        if(goal!=SupplyPlan.Kind.NONE){
            explorationFrame=null;
            if(now<retrySupply){info.setText(" Comprobando inventario ");schedule(650);return;}
            float[] item=findSupply(goal,hud);
            if(item!=null){
                byte[] patch=VisualCues.patch(rgb,item[0],item[1]);
                pendingAppearance=items.locations[goal.ordinal()]!=null && VisualCues.matches(patch,patch)?patch:null;
                pendingItem=goal;verifyUntil=now+4500;retrySupply=verifyUntil;
                info.setText(" Recoger "+goal.name()+" ");
                perform(id,item[0],item[1],item[0],item[1],90,()->schedule(650));return;
            }
            // Alternate travel and scanning while the need persists; no known world map is assumed.
            boolean look=++searchSteps%4==0;
            float x=hud.getFloat(look?"extraX4":"extraX3",look?.67f:.15f);
            float y=hud.getFloat(look?"extraY4":"extraY3",look?.48f:.77f);
            info.setText(goal==SupplyPlan.Kind.UNKNOWN?" Buscando tipo de arma ":" Buscar "+goal.name()+" ");
            perform(id,x,y,(float)clamp(x+(look?.12:0),.01,.99),
                (float)clamp(y-(look?0:.14),.01,.99),look?250:800,()->schedule(650));return;
        }
        if(lootFrames>=2){''')
shutil.copyfile(Path(__file__).with_name('SupplyPlan.java'),live/'SupplyPlan.java')
path.write_text(s)
