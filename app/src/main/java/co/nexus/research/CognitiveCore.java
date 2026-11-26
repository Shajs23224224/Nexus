package co.nexus.research;
import java.io.*;
import java.util.*;
/** Sparse action-conditioned experts for local simulation. */
public final class CognitiveCore {
 public static final int INPUT=144,HIDDEN=128,HEADS=4,EXPERTS=64,PARAMETERS=EXPERTS*((INPUT+1)*HIDDEN+(HIDDEN+1)*HIDDEN+(HIDDEN+1)*HEADS);
 public enum Action {OBSERVE,SCAN,EXPLORE,MOVE,GET_ENERGY,GET_KIT,RECOVER,SHELTER,TRACK,TAG,RECHARGE,DRIVE,EXIT,READ,INTERACT,PLAN}
 public static final class Observation {
  final float[] state;final EnumSet<Action> allowed;
  public Observation(float[] x,Set<Action> a){if(x.length!=128)throw new IllegalArgumentException("state length");state=x.clone();for(float v:state)if(!Float.isFinite(v)||v<0||v>1)throw new IllegalArgumentException("state value");allowed=EnumSet.noneOf(Action.class);allowed.addAll(a);}
  float[] input(Action a){float[] x=Arrays.copyOf(state,INPUT);x[128+a.ordinal()]=1;return x;}
 }
 public static final class Decision {public final Action action;public final float[] heads;public final float score,uncertainty;Decision(Action a,float[] h,float s,float u){action=a;heads=h;score=s;uncertainty=u;}}
 static final class Expert {
  final float[] a=new float[(INPUT+1)*HIDDEN],b=new float[(HIDDEN+1)*HIDDEN],c=new float[(HIDDEN+1)*HEADS];long updates;
  Expert(Random r){init(a,INPUT,HIDDEN,r);init(b,HIDDEN,HIDDEN,r);init(c,HIDDEN,HEADS,r);}
  static void init(float[] w,int n,int m,Random r){float scale=(float)Math.sqrt(6.0/(n+m));for(int j=0;j<m;j++)for(int i=0;i<n;i++)w[j*(n+1)+i]=(r.nextFloat()*2-1)*scale;}
  static float[] layer(float[] x,float[] w,int m,boolean sig){float[] y=new float[m];int n=x.length;for(int j=0;j<m;j++){int k=j*(n+1);float z=w[k+n];for(int i=0;i<n;i++)z+=w[k+i]*x[i];y[j]=sig?(float)(1/(1+Math.exp(-z))):(float)Math.tanh(z);}return y;}
  float[] predict(float[] x){return layer(layer(layer(x,a,HIDDEN,false),b,HIDDEN,false),c,HEADS,true);}
  void train(float[] x,float[] target){float[] h=layer(x,a,HIDDEN,false),k=layer(h,b,HIDDEN,false),y=layer(k,c,HEADS,true),d=new float[HEADS],e=new float[HIDDEN],f=new float[HIDDEN];for(int j=0;j<HEADS;j++)d[j]=(y[j]-target[j])*y[j]*(1-y[j]);for(int i=0;i<HIDDEN;i++){float z=0;for(int j=0;j<HEADS;j++)z+=d[j]*c[j*(HIDDEN+1)+i];e[i]=z*(1-k[i]*k[i]);}for(int i=0;i<HIDDEN;i++){float z=0;for(int j=0;j<HIDDEN;j++)z+=e[j]*b[j*(HIDDEN+1)+i];f[i]=z*(1-h[i]*h[i]);}step(c,k,d);step(b,h,e);step(a,x,f);updates++;}
  static void step(float[] w,float[] x,float[] d){int n=x.length;for(int j=0;j<d.length;j++){int k=j*(n+1);for(int i=0;i<n;i++)w[k+i]-=.025f*d[j]*x[i];w[k+n]-=.025f*d[j];}}
 }
 private final Expert[] experts=new Expert[EXPERTS];private final Random rng;private final ArrayDeque<Experience> replay=new ArrayDeque<>();public long updates,episodes,wins;
 static final class Experience{final Observation o;final Action a;final float[] y;Experience(Observation o,Action a,float[] y){this.o=o;this.a=a;this.y=y.clone();}}
 public CognitiveCore(long seed){rng=new Random(seed);for(int i=0;i<EXPERTS;i++)experts[i]=new Expert(new Random(seed+104729L*i));}
 static int group(Action a){switch(a){case OBSERVE:case SCAN:return 0;case EXPLORE:case MOVE:return 1;case GET_ENERGY:case GET_KIT:return 2;case RECOVER:case SHELTER:return 3;case TRACK:case TAG:case RECHARGE:return 4;case DRIVE:case EXIT:return 5;case READ:case INTERACT:return 6;default:return 7;}}
 static int route(Observation o,Action a){return group(a)*8+(o.state[3]>=.5f?4:0);}
 public List<Decision> rank(Observation o){List<Decision> list=new ArrayList<>();for(Action a:o.allowed){int base=route(o,a);float[] x=o.input(a),m=new float[4];float[][] p=new float[4][];for(int i=0;i<4;i++){p[i]=experts[base+i].predict(x);for(int j=0;j<4;j++)m[j]+=p[i][j]*.25f;}float v=0;for(float[] q:p)for(int j=0;j<4;j++)v+=(q[j]-m[j])*(q[j]-m[j])/16;float u=(float)Math.sqrt(v);float score=.45f*m[0]+(.25f+.35f*Math.max(1-o.state[0],o.state[3]))*m[1]-.15f*m[2]+.15f*m[3]-.2f*u;list.add(new Decision(a,m,score,u));}list.sort((a,b)->Float.compare(b.score,a.score));return list;}
 public Decision choose(Observation o,boolean train){List<Decision> d=rank(o);if(d.isEmpty())throw new IllegalArgumentException("No valid action");double eps=Math.max(.10,.85*Math.exp(-updates/1800.0));return train&&rng.nextDouble()<eps?d.get(rng.nextInt(d.size())):d.get(0);}
 public void learn(Observation o,Action a,float[] y){if(!o.allowed.contains(a)||y.length!=4)throw new IllegalArgumentException("outcome");for(float v:y)if(!Float.isFinite(v)||v<0||v>1)throw new IllegalArgumentException("label");Experience e=new Experience(o,a,y);if(replay.size()==256)replay.removeFirst();replay.addLast(e);fit(e);if(replay.size()>8){int k=rng.nextInt(replay.size());for(Experience z:replay)if(k--==0){fit(z);break;}}updates++;}
 private void fit(Experience e){int b=route(e.o,e.a);float[] x=e.o.input(e.a);for(int i=0;i<4;i++)experts[b+i].train(x,e.y);}
 public void write(OutputStream stream)throws IOException{DataOutputStream o=new DataOutputStream(new BufferedOutputStream(stream));o.writeInt(0x4e583134);o.writeInt(PARAMETERS);o.writeLong(updates);o.writeLong(episodes);o.writeLong(wins);for(Expert e:experts){o.writeLong(e.updates);for(float[] w:new float[][]{e.a,e.b,e.c})for(float v:w)o.writeFloat(v);}o.flush();}
 public static CognitiveCore read(InputStream stream)throws IOException{DataInputStream i=new DataInputStream(new BufferedInputStream(stream));if(i.readInt()!=0x4e583134||i.readInt()!=PARAMETERS)throw new IOException("Checkpoint incompatible");CognitiveCore c=new CognitiveCore(1);c.updates=i.readLong();c.episodes=i.readLong();c.wins=i.readLong();if(c.updates<0||c.episodes<0||c.wins<0||c.wins>c.episodes)throw new IOException("Bad counters");for(Expert e:c.experts){e.updates=i.readLong();if(e.updates<0)throw new IOException("Bad counter");for(float[] w:new float[][]{e.a,e.b,e.c})for(int k=0;k<w.length;k++){float v=i.readFloat();if(!Float.isFinite(v))throw new IOException("Bad weight");w[k]=v;}}if(i.read()!=-1)throw new IOException("Trailing data");return c;}
}
