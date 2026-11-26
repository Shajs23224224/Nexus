package co.nexus.research;
import java.io.*;
import java.util.*;

/** Independent sparse MLPs. Eight 1024-unit layers; fixed sparse topology, trainable weights. */
public final class SparseBrain {
 public static final int NETWORKS=412,AGENTS=80,LAYERS=8,WIDTH=1024,HEADS=16,INPUT=144,FAN=4,OUT_FAN=64;
 public static final int PER_NET=LAYERS*WIDTH*(FAN+1)+HEADS*(OUT_FAN+1),PARAMETERS=NETWORKS*PER_NET;
 public static final String[] HEAD_NAMES={"Utilidad","Salud","Coste","Información","Progreso","Energía","Kits","Movilidad","Lectura","Interacción","Seguimiento","Seguridad","Éxito","Continuidad","Tiempo restante","Novedad"};
 private final Net[] nets=new Net[NETWORKS];private final long seed;private final Random rng;
 private final ArrayDeque<Experience> replay=new ArrayDeque<>();
 public long updates,episodes,wins,videoPairs;
 static final class Experience{final CognitiveCore.Observation o;final CognitiveCore.Action a;final float[] y;Experience(CognitiveCore.Observation o,CognitiveCore.Action a,float[] y){this.o=o;this.a=a;this.y=y.clone();}}
 public static final class Decision{public final CognitiveCore.Action action;public final float[] heads;public final float score,uncertainty;public final int agent;Decision(CognitiveCore.Action a,float[] h,float s,float u,int g){action=a;heads=h;score=s;uncertainty=u;agent=g;}}
 static final class Net{
  final int id;final float[] w=new float[PER_NET];long updates;
  Net(int id,long seed){this.id=id;Random r=new Random(seed+104729L*id);for(int k=0;k<LAYERS*WIDTH;k++)for(int j=0;j<FAN;j++)w[k*(FAN+1)+j]=(r.nextFloat()*2-1)*.65f;int p=LAYERS*WIDTH*(FAN+1);for(int k=0;k<HEADS;k++)for(int j=0;j<OUT_FAN;j++)w[p+k*(OUT_FAN+1)+j]=(r.nextFloat()*2-1)*.15f;}
  int src(int l,int j,int k){return l==0?Math.floorMod(j*13+k*31+id*7,INPUT):(j+k*257+id*17+l*37)&1023;}
  int osrc(int j,int k){return (j*64+k+id*19)&1023;}
  float[][] forward(float[] x){float[][] h=new float[LAYERS+2][];h[0]=x;for(int l=0;l<LAYERS;l++){float[] a=h[l],b=new float[WIDTH];for(int j=0;j<WIDTH;j++){int p=(l*WIDTH+j)*(FAN+1);float z=w[p+FAN];for(int k=0;k<FAN;k++)z+=w[p+k]*a[src(l,j,k)];float t=(float)Math.tanh(z);b[j]=l==0?t:.5f*t+.5f*a[j];}h[l+1]=b;}float[] out=new float[HEADS];int base=LAYERS*WIDTH*(FAN+1);for(int j=0;j<HEADS;j++){int p=base+j*(OUT_FAN+1);float z=w[p+OUT_FAN];for(int k=0;k<OUT_FAN;k++)z+=w[p+k]*h[LAYERS][osrc(j,k)];out[j]=(float)(1/(1+Math.exp(-z)));}h[LAYERS+1]=out;return h;}
  float[] predict(float[] x){return forward(x)[LAYERS+1];}
  void train(float[] x,float[] y){float[][] h=forward(x);float[] d=new float[WIDTH];int base=LAYERS*WIDTH*(FAN+1);float lr=.04f;for(int j=0;j<HEADS;j++){int p=base+j*(OUT_FAN+1);float out=h[LAYERS+1][j],g=(out-y[j])*out*(1-out);for(int k=0;k<OUT_FAN;k++){int s=osrc(j,k);d[s]+=g*w[p+k];w[p+k]-=lr*g*h[LAYERS][s];}w[p+OUT_FAN]-=lr*g;}
   for(int l=LAYERS-1;l>=0;l--){float[] prev=h[l],next=new float[prev.length];for(int j=0;j<WIDTH;j++){int p=(l*WIDTH+j)*(FAN+1);float t=l==0?h[l+1][j]:2*h[l+1][j]-prev[j];t=Math.max(-1,Math.min(1,t));float g=d[j]*(1-t*t)*(l==0?1:.5f);if(l>0)next[j]+=.5f*d[j];for(int k=0;k<FAN;k++){int s=src(l,j,k);next[s]+=g*w[p+k];w[p+k]-=lr*g*prev[s];}w[p+FAN]-=lr*g;}d=next;}updates++;
  }
 }
 public SparseBrain(long seed){this.seed=seed;rng=new Random(seed);}
 private Net net(int id){if(nets[id]==null)nets[id]=new Net(id,seed);return nets[id];}
 public int allocated(){int n=0;for(Net e:nets)if(e!=null)n++;return n;}
 public int trained(){int n=0;for(Net e:nets)if(e!=null&&e.updates>0)n++;return n;}
 // 16 action roles × 5 contexts = 80 routing agents; five independently weighted networks each.
 public static int agent(CognitiveCore.Observation o,CognitiveCore.Action a){float[] s=o.state;int context=s[0]<.55f?1:s[3]>.5f?2:s[1]<.21f?3:s[10]>.5f?4:0;return a.ordinal()*5+context;}
 static int[] route(CognitiveCore.Observation o,CognitiveCore.Action a){int base=agent(o,a)*5;int k=Math.floorMod(Math.round(o.state[13]*7)+Math.round(o.state[14]*7)*8+Math.round(o.state[9]*80),5);return new int[]{base+k,base+(k+1)%5};}
 public List<Decision> rank(CognitiveCore.Observation o){List<Decision> list=new ArrayList<>();for(CognitiveCore.Action a:o.allowed){int[] ids=route(o,a);float[] x=o.input(a),p=net(ids[0]).predict(x),q=net(ids[1]).predict(x),m=new float[HEADS];float v=0;for(int j=0;j<HEADS;j++){m[j]=(p[j]+q[j])*.5f;v+=(p[j]-q[j])*(p[j]-q[j])/(4*HEADS);}float u=(float)Math.sqrt(v);float score=.34f*m[0]+(.15f+.25f*Math.max(1-o.state[0],o.state[3]))*m[1]-.12f*m[2]+.06f*m[3]+.14f*m[4]+.10f*m[9]+.10f*m[12]+.08f*m[13]+.04f*m[15]-.18f*u;list.add(new Decision(a,m,score,u,agent(o,a)));}list.sort((a,b)->Float.compare(b.score,a.score));return list;}
 public Decision choose(CognitiveCore.Observation o,boolean train){List<Decision> d=rank(o);if(d.isEmpty())throw new IllegalArgumentException("No valid action");double eps=Math.max(.12,.85*Math.exp(-updates/2400.0));return train&&rng.nextDouble()<eps?d.get(rng.nextInt(d.size())):d.get(0);}
 static void check(float[] v,int n){if(v.length!=n)throw new IllegalArgumentException("dimension");for(float f:v)if(!Float.isFinite(f)||f<0||f>1)throw new IllegalArgumentException("range");}
 public void learn(CognitiveCore.Observation o,CognitiveCore.Action a,float[] y){check(y,HEADS);if(!o.allowed.contains(a))throw new IllegalArgumentException("action");Experience e=new Experience(o,a,y);if(replay.size()==256)replay.removeFirst();replay.addLast(e);fit(e);if(replay.size()>8){int k=rng.nextInt(replay.size());for(Experience z:replay)if(k--==0){fit(z);break;}}updates++;}
 private void fit(Experience e){float[] x=e.o.input(e.a);for(int id:route(e.o,e.a))net(id).train(x,e.y);}
 public static float[] outcome(float[] basic,CognitiveArena a){float[] s=a.observe().state;return new float[]{basic[0],basic[1],basic[2],basic[3],s[8],s[1],s[2],s[10],s[11],s[12],s[4],1-s[3],a.won?1:0,a.health>0?1:0,1-s[9],basic[3]};}
 // Last 12 networks learn visual prediction, independently of decision labels.
 public static float[] videoInput(float[] f){check(f,128);return Arrays.copyOf(f,INPUT);}
 public static float[] videoTarget(float[] f){check(f,128);float[] y=new float[HEADS];for(int i=0;i<128;i++)y[i/8]+=f[i]/8;return y;}
 public float[] predictVideo(float[] frame,int group){int id=400+2*Math.floorMod(group,6);float[] x=videoInput(frame),a=net(id).predict(x),b=net(id+1).predict(x);for(int j=0;j<HEADS;j++)a[j]=(a[j]+b[j])*.5f;return a;}
 public void learnVideo(float[] frame,float[] next,int group){int id=400+2*Math.floorMod(group,6);float[] x=videoInput(frame),y=videoTarget(next);net(id).train(x,y);net(id+1).train(x,y);videoPairs++;}
 public void write(OutputStream stream)throws IOException{DataOutputStream o=new DataOutputStream(new BufferedOutputStream(stream));o.writeInt(0x4e583135);o.writeInt(PARAMETERS);o.writeLong(seed);o.writeLong(updates);o.writeLong(episodes);o.writeLong(wins);o.writeLong(videoPairs);o.writeInt(trained());for(Net e:nets)if(e!=null&&e.updates>0){o.writeInt(e.id);o.writeLong(e.updates);for(float f:e.w)o.writeFloat(f);}o.flush();}
 public static SparseBrain read(InputStream stream)throws IOException{DataInputStream i=new DataInputStream(new BufferedInputStream(stream));if(i.readInt()!=0x4e583135||i.readInt()!=PARAMETERS)throw new IOException("Incompatible model");SparseBrain b=new SparseBrain(i.readLong());b.updates=i.readLong();b.episodes=i.readLong();b.wins=i.readLong();b.videoPairs=i.readLong();int n=i.readInt();if(n<0||n>NETWORKS||b.updates<0||b.episodes<0||b.wins<0||b.wins>b.episodes||b.videoPairs<0)throw new IOException("Bad checkpoint");for(int j=0;j<n;j++){int id=i.readInt();if(id<0||id>=NETWORKS||b.nets[id]!=null)throw new IOException("Invalid expert");Net e=b.net(id);e.updates=i.readLong();if(e.updates<=0)throw new IOException("Invalid update count");for(int k=0;k<PER_NET;k++){float f=i.readFloat();if(!Float.isFinite(f))throw new IOException("Invalid weight");e.w[k]=f;}}if(i.read()!=-1)throw new IOException("Trailing data");return b;}
}
