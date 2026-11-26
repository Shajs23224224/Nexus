package co.nexus.lab.live;

public class SupplyCheck {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        SupplyPlan plan=new SupplyPlan();
        SupplyPlan.Frame frame=new SupplyPlan.Frame();
        frame.observe("Sin munición",0,0);plan.update(frame);
        check(plan.goal(1)==SupplyPlan.Kind.UNKNOWN);
        frame=new SupplyPlan.Frame();
        frame.observe("Arma equipada: AR",0,0);frame.observe("Recoger munición SMG",.8f,.4f);
        plan.update(frame);check(plan.goal(1)==SupplyPlan.Kind.AR);
        check(frame.locations[SupplyPlan.Kind.AR.ordinal()]==null);
        frame.observe("Recoger munición AR",.8f,.5f);
        check(frame.locations[SupplyPlan.Kind.AR.ordinal()][1]==.5f);
        frame.observe("Sin botiquines",0,0);plan.update(frame);
        check(plan.goal(.3f)==SupplyPlan.Kind.KIT);
        plan.update(new SupplyPlan.Frame());check(plan.goal(.3f)==SupplyPlan.Kind.KIT);
        frame=new SupplyPlan.Frame();frame.observe("Botiquines: 1",0,0);plan.update(frame);
        check(plan.goal(.3f)==SupplyPlan.Kind.AR);
        frame.observe("Munición de reserva: 30",0,0);plan.update(frame);
        check(plan.goal(1)==SupplyPlan.Kind.NONE);
    }
}