import co.nexus.research.*;import java.io.*;import java.util.*;
public class SparseTest {
 static int passed;static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);passed++;System.out.println("PASS "+s);}
 static float mse(float[] a,float[] b){float e=0;for(int i=0;i<a.length;i++)e+=(a[i]-b[i])*(a[i]-b[i]);return e/a.length;}
 public static void main(String[] args)throws Exception{
  SparseBrain b=new SparseBrain(15);check(SparseBrain.NETWORKS==412&&SparseBrain.AGENTS==80&&SparseBrain.LAYERS==8&&SparseBrain.WIDTH==1024&&SparseBrain.HEADS==16&&SparseBrain.PARAMETERS==17304000,"architecture");check(b.allocated()==0,"lazy allocation");
  float[] s=new float[128];s[0]=1;s[1]=.5f;CognitiveCore.Observation o=new CognitiveCore.Observation(s,EnumSet.of(CognitiveCore.Action.READ));float[] y=new float[16];Arrays.fill(y,.85f);
  float before=mse(b.rank(o).get(0).heads,y);for(int i=0;i<80;i++)b.learn(o,CognitiveCore.Action.READ,y);float after=mse(b.rank(o).get(0).heads,y);check(after<before,"train eight-layer network");check(b.allocated()==2&&b.trained()==2,"selective routing");
  Set<Integer> roles=new HashSet<>();for(CognitiveCore.Action a:CognitiveCore.Action.values())for(int k=0;k<5;k++){float[] f=s.clone();if(k==1)f[0]=.4f;if(k==2)f[3]=1;if(k==3)f[1]=.1f;if(k==4)f[10]=1;roles.add(SparseBrain.agent(new CognitiveCore.Observation(f,EnumSet.of(a)),a));}check(roles.size()==80,"all 80 routing roles");
  check(b.rank(o).size()==1&&b.rank(o).get(0).heads.length==16,"action mask and intermediate heads");
  float[] frame=new float[128],next=new float[128];Arrays.fill(frame,.3f);Arrays.fill(next,.9f);float v0=mse(b.predictVideo(frame,0),SparseBrain.videoTarget(next));for(int i=0;i<60;i++)b.learnVideo(frame,next,0);float v1=mse(b.predictVideo(frame,0),SparseBrain.videoTarget(next));check(v1<v0&&b.videoPairs==60,"self-supervised video prediction");
  ByteArrayOutputStream out=new ByteArrayOutputStream();b.write(out);SparseBrain c=SparseBrain.read(new ByteArrayInputStream(out.toByteArray()));check(Arrays.equals(b.rank(o).get(0).heads,c.rank(o).get(0).heads)&&Arrays.equals(b.predictVideo(frame,0),c.predictVideo(frame,0))&&c.videoPairs==60,"exact checkpoint for decision and video");
  long u=c.updates;CognitiveArena arena=new CognitiveArena(40000);while(!arena.done){CognitiveCore.Observation obs=arena.observe();SparseBrain.Decision d=c.choose(obs,false);arena.step(d.action);}check(c.updates==u&&arena.steps<=80,"bounded read-only evaluation");
  CognitiveArena train=new CognitiveArena(123);for(int i=0;i<16&&!train.done;i++){CognitiveCore.Observation obs=train.observe();SparseBrain.Decision d=c.choose(obs,true);c.learn(obs,d.action,SparseBrain.outcome(train.step(d.action),train));}check(c.updates>u,"autonomous simulator updates");System.out.println("Decision MSE "+before+" -> "+after+"; video MSE "+v0+" -> "+v1+"; "+passed+" checks");
 }
}
