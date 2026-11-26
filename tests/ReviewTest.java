import co.nexus.research.*;import java.io.*;import java.util.*;import java.nio.file.*;
public class ReviewTest {
 static int passed;static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);passed++;System.out.println("PASS "+s);}
 public static void main(String[] args)throws Exception{
  SparseBrain b=new SparseBrain(15);CognitiveArena a=new CognitiveArena(123);
  StrategyLab.Plan p=StrategyLab.propose(a);check(p.sequence.size()<=4&&p.sequence.size()>0&&a.steps==0,"counterfactual planning does not mutate world");
  a.step(p.action);CognitiveArena fork=a.fork();check(fork.pos==a.pos&&fork.health==a.health&&fork.steps==a.steps&&fork.history().equals(a.history()),"deterministic replay");
  ByteArrayOutputStream demo=new ByteArrayOutputStream();StrategyLab.exportDemo(demo,a);long u=b.updates;StrategyLab.Review review=StrategyLab.importDemo(new ByteArrayInputStream(demo.toByteArray()),b,false);check(review.steps==1&&review.alternatives>0&&b.updates==u,"review without learning");StrategyLab.importDemo(new ByteArrayInputStream(demo.toByteArray()),b,true);check(b.updates==u+1,"demonstration trains decision model");
  try{StrategyLab.importDemo(new ByteArrayInputStream("NXDEMO1\n123\nMOVE\nFAKE\n".getBytes("UTF-8")),b,true);throw new AssertionError("invalid accepted");}catch(IOException expected){}check(b.updates==u+1,"invalid demonstrations rejected before training");
  float[] state=new float[128];state[0]=1;state[1]=.5f;CognitiveCore.Observation o=new CognitiveCore.Observation(state,EnumSet.of(CognitiveCore.Action.READ));float[] y=new float[16];Arrays.fill(y,.8f);for(int i=0;i<12;i++)b.learn(o,CognitiveCore.Action.READ,y);float[] original=b.rank(o).get(0).heads,quant=b.rankQuantized(o).get(0).heads;float max=0;for(int k=0;k<16;k++)max=Math.max(max,Math.abs(original[k]-quant[k]));check(max<.04,"INT8 weights approximation within 0.04");
  File dir=Files.createTempDirectory("nx-cache").toFile();b.enableDiskCache(dir,2);for(CognitiveCore.Action x:CognitiveCore.Action.values())b.rank(new CognitiveCore.Observation(state,EnumSet.of(x)));check(b.allocated()<=2,"bounded expert residency");check(Arrays.equals(original,b.rank(o).get(0).heads),"exact disk cache restore");
  ByteArrayOutputStream out=new ByteArrayOutputStream();b.write(out);SparseBrain copy=SparseBrain.read(new ByteArrayInputStream(out.toByteArray()));check(Arrays.equals(original,copy.rank(o).get(0).heads)&&copy.trained()==b.trained(),"checkpoint includes evicted weights");for(File f:dir.listFiles())f.delete();dir.delete();System.out.println("Review checks: "+passed+" | quantization max error "+max);
 }
}
