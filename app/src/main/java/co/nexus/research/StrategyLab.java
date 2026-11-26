package co.nexus.research;
import java.io.*;
import java.util.*;
/** Evaluates action sequences against the deterministic local simulator only. */
public final class StrategyLab {
 public static final class Review {
  public final int steps,alternatives,errors;public final boolean won;public final float regret;
  Review(int s,int a,int e,boolean w,float r){steps=s;alternatives=a;errors=e;won=w;regret=r;}
  public String toString(){return "Demostración: "+steps+" pasos | alternativas "+alternatives+" | errores potenciales "+errors+" | éxito "+won+" | diferencia "+String.format(Locale.ROOT,"%.3f",regret);}
 }
 public static final class Plan {
  public final CognitiveCore.Action action;public final List<CognitiveCore.Action> sequence;public final float value;
  Plan(List<CognitiveCore.Action> a,float v){action=a.get(0);sequence=Collections.unmodifiableList(new ArrayList<>(a));value=v;}
 }
 private static float value(CognitiveArena a,float[] y){float[] s=a.observe().state;return .45f*y[0]+.22f*s[0]+.18f*s[8]+.10f*s[12]+.05f*y[3]+(a.won?1:0)-(a.done&&!a.won?.8f:0);}
 private static final CognitiveCore.Action[] PRIORITY={CognitiveCore.Action.RECOVER,CognitiveCore.Action.GET_KIT,CognitiveCore.Action.GET_ENERGY,CognitiveCore.Action.READ,CognitiveCore.Action.INTERACT,CognitiveCore.Action.DRIVE,CognitiveCore.Action.MOVE,CognitiveCore.Action.OBSERVE};
 static CognitiveCore.Action follow(CognitiveArena a,int mode){Set<CognitiveCore.Action> allowed=a.observe().allowed;if(mode==1&&allowed.contains(CognitiveCore.Action.SCAN)&&a.steps%3==0)return CognitiveCore.Action.SCAN;if(mode==2&&allowed.contains(CognitiveCore.Action.EXPLORE)&&a.steps%3==0)return CognitiveCore.Action.EXPLORE;for(CognitiveCore.Action x:PRIORITY)if(allowed.contains(x))return x;return allowed.iterator().next();}
 public static Plan simulate(CognitiveArena source,CognitiveCore.Action first,int mode){CognitiveArena a=source.fork();List<CognitiveCore.Action> seq=new ArrayList<>();float sum=0,discount=1;for(int i=0;i<4&&!a.done;i++){CognitiveCore.Action x=i==0?first:follow(a,mode);seq.add(x);sum+=discount*value(a,a.step(x));discount*=.8f;}return new Plan(seq,sum);}
 public static Plan propose(CognitiveArena a){Plan best=null;for(CognitiveCore.Action first:a.observe().allowed)for(int mode=0;mode<3;mode++){Plan p=simulate(a,first,mode);if(best==null||p.value>best.value)best=p;}return best;}
 /** Blend learned utility with an independent four-step simulator check. */
 public static SparseBrain.Decision choose(SparseBrain b,CognitiveArena a,boolean train){if(train)return b.choose(a.observe(),true);List<SparseBrain.Decision> ranks=b.rankQuantized(a.observe());SparseBrain.Decision best=null;float score=-Float.MAX_VALUE;for(SparseBrain.Decision d:ranks){float v=d.score+.35f*simulate(a,d.action,0).value;if(v>score){best=d;score=v;}}return best;}
 /** Format: NXDEMO1 then seed, then one action name per line. Only replayable local arena actions. */
 public static Review importDemo(InputStream in,SparseBrain brain,boolean train)throws IOException{
  BufferedReader r=new BufferedReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8));if(!"NXDEMO1".equals(r.readLine()))throw new IOException("Formato NXDEMO1 requerido");long seed;try{seed=Long.parseLong(r.readLine());}catch(RuntimeException e){throw new IOException("Semilla inválida",e);}
  CognitiveArena a=new CognitiveArena(seed);List<CognitiveCore.Action> actions=new ArrayList<>();String line;while((line=r.readLine())!=null){if(line.trim().isEmpty())continue;if(actions.size()>=80||a.done)throw new IOException("Demostración excede episodio");try{CognitiveCore.Action x=CognitiveCore.Action.valueOf(line.trim());a.step(x);actions.add(x);}catch(RuntimeException e){throw new IOException("Acción inválida en paso "+(actions.size()+1),e);}}if(actions.isEmpty())throw new IOException("Sin acciones");
  a=new CognitiveArena(seed);float regret=0;int errors=0,alts=0;for(CognitiveCore.Action x:actions){CognitiveCore.Observation o=a.observe();Plan chosen=simulate(a,x,0),best=propose(a);float gap=Math.max(0,best.value-chosen.value);regret+=gap;if(gap>.18f)errors++;alts+=o.allowed.size()*3;float[] y=SparseBrain.outcome(a.step(x),a);if(train)brain.learn(o,x,y);}
  return new Review(actions.size(),alts,errors,a.won,regret/actions.size());
 }
 public static void exportDemo(OutputStream out,CognitiveArena a)throws IOException{BufferedWriter w=new BufferedWriter(new OutputStreamWriter(out,java.nio.charset.StandardCharsets.UTF_8));w.write("NXDEMO1\n"+a.seed+"\n");for(CognitiveCore.Action x:a.history())w.write(x.name()+"\n");w.flush();}
}
