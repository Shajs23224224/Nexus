package co.nexus.lab.live;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keep missing supplies as goals across frames. Only explicit inventory observations clear them. */
final class SupplyPlan {
    enum Kind { NONE, KIT, AR, SMG, SG, SR, HG, UNKNOWN }
    private static final Pattern COUNT=Pattern.compile("(botiquines|medkits|municion de reserva|reserve ammo)\\s*[:x]\\s*(\\d{1,3})");
    static class Frame {
        Kind equipped=Kind.UNKNOWN;
        int kits=-1,reserve=-1;
        boolean outOfKits,outOfAmmo;
        final float[][] locations=new float[Kind.values().length][];
        void observe(String raw,float x,float y){
            String s=normalize(raw);
            if(s.equals("sin botiquines")||s.equals("no medkits"))outOfKits=true;
            if(s.equals("sin municion")||s.equals("out of ammo"))outOfAmmo=true;
            Matcher m=COUNT.matcher(s);
            if(m.matches()){
                int n=Integer.parseInt(m.group(2));
                if(s.startsWith("botiquines")||s.startsWith("medkits"))kits=n;else reserve=n;
            }
            if(s.startsWith("arma equipada:")||s.startsWith("equipped weapon:")||s.startsWith("tipo de municion:"))
                equipped=family(s.substring(s.indexOf(':')+1));
            Kind pickup=Kind.NONE;
            if(s.matches("(recoger|pick up) (botiquin|medkit)"))pickup=Kind.KIT;
            else if(s.startsWith("recoger municion ")||s.startsWith("pick up ") && s.endsWith(" ammo"))
                pickup=family(s);
            if(pickup!=Kind.NONE && pickup!=Kind.UNKNOWN && x>0 && x<1 && y>0 && y<1)
                locations[pickup.ordinal()]=new float[]{x,y};
        }
    }
    static String normalize(String raw){
        if(raw==null || raw.length()>96)return "";
        return Normalizer.normalize(raw,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim();
    }
    static Kind family(String s){
        for(String word:s.toUpperCase(Locale.ROOT).split("[^A-Z0-9]+")){
            switch(word){
                case "AR":return Kind.AR;case "SMG":return Kind.SMG;
                case "SG":return Kind.SG;case "SR":return Kind.SR;case "HG":return Kind.HG;
            }
        }
        return Kind.UNKNOWN;
    }
    boolean needKits, needAmmo;
    Kind equipped=Kind.UNKNOWN;
    void update(Frame f){
        if(f.equipped!=Kind.UNKNOWN)equipped=f.equipped;
        if(f.kits>=0)needKits=f.kits==0;
        if(f.reserve>=0)needAmmo=f.reserve==0;
        if(f.outOfKits)needKits=true;
        if(f.outOfAmmo)needAmmo=true;
    }
    Kind goal(float hp){
        if(needKits && (!needAmmo || hp>=0 && hp<.6f || equipped==Kind.UNKNOWN))return Kind.KIT;
        return needAmmo?equipped:Kind.NONE;
    }
}