package co.nexus.research;
import java.util.*;
import java.io.*;
/** Compact Q-learning agent with persistent weights, restricted to local research simulator. */
public final class Agent {
 public static final int N=8, S=64, ACTIONS=4;
 public final double[][] q=new double[S*2][ACTIONS];
 private final Random rng=new Random(73);
 public int episodes,wins,updates;
 public int choose(int s,boolean train){if(train && rng.nextDouble()<Math.max(.08,.7*Math.exp(-episodes/150.0)))return rng.nextInt(ACTIONS); int a=0; for(int i=1;i<ACTIONS;i++)if(q[s][i]>q[s][a])a=i;return a;}
 public void learn(int s,int a,double r,int next,boolean done){double max=0;if(!done){max=q[next][0];for(double x:q[next])max=Math.max(max,x);}q[s][a]+=.15*(r+.95*max-q[s][a]);updates++;}
 public void save(OutputStream stream)throws IOException{try(DataOutputStream out=new DataOutputStream(stream)){out.writeInt(0x4e583036);out.writeInt(episodes);out.writeInt(wins);out.writeInt(updates);for(double[] row:q)for(double x:row)out.writeDouble(x);}}
 public void load(InputStream stream)throws IOException{try(DataInputStream in=new DataInputStream(stream)){if(in.readInt()!=0x4e583036)throw new IOException("format");int e=in.readInt(),w=in.readInt(),u=in.readInt();if(e<0||w<0||w>e||u<0)throw new IOException("counters");double[][] tmp=new double[q.length][ACTIONS];for(double[] row:tmp)for(int a=0;a<ACTIONS;a++){row[a]=in.readDouble();if(!Double.isFinite(row[a]))throw new IOException("weight");}for(int i=0;i<q.length;i++)System.arraycopy(tmp[i],0,q[i],0,ACTIONS);episodes=e;wins=w;updates=u;}}
 public static final class Arena {
  public int pos=0,steps=0;public boolean has=false,done=false,won=false;
  public int state(){return pos+(has?64:0);}
  public double step(int a){if(done||a<0||a>=4)throw new IllegalStateException();int x=pos%8,y=pos/8;switch(a){case 0:y--;break;case 1:x++;break;case 2:y++;break;case 3:x--;break;}double r=-.05;if(x<0||y<0||x>=8||y>=8)r-=.3;else{int n=y*8+x;if(n==18||n==26||n==34||n==42)r-=.3;else pos=n;}if(pos==7&&!has){has=true;r+=2;}if(pos==63&&has){won=done=true;r+=6;}if(++steps>=100)done=true;return r;}
 }
}