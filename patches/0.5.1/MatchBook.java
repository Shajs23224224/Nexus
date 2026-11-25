package co.nexus.lab.live;

import java.io.*;
import java.util.*;

/** Small on-device classifier, compatible with the visual profiles saved by NEXUS 0.5. */
public final class MatchBook {
    public static final String[] PHASES={"Lobby","Despliegue","Partida","Resultados"};
    static final int LENGTH=16*9*3,MAX=160;
    private static final int MAGIC=0x4e584d31;
    static final class Example {
        final int phase,millis;
        final byte[] visual;
        final float x,y,u,v;
        Example(int phase,byte[] visual,float x,float y,float u,float v,int millis){
            this.phase=phase;this.visual=visual.clone();this.x=x;this.y=y;this.u=u;this.v=v;this.millis=millis;
        }
    }
    final ArrayList<Example> examples=new ArrayList<>();
    private Example pending;
    private int votes;
    public void resetDecision(){pending=null;votes=0;}
    public static byte[] describe(int[] pixels,int w,int h){
        byte[] out=new byte[LENGTH];int k=0;
        for(int row=0;row<9;row++)for(int col=0;col<16;col++){
            int r=0,g=0,b=0;
            for(int j=0;j<4;j++)for(int i=0;i<4;i++){
                int x=(col*8+2*i+1)*w/128,y=h/7+(row*8+2*j+1)*(h-h/7)/72;
                int c=pixels[y*w+x];r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;
            }
            out[k++]=(byte)(r/16);out[k++]=(byte)(g/16);out[k++]=(byte)(b/16);
        }
        return out;
    }
    private static boolean coord(float p){return p>=0&&p<=1;}
    public void add(int phase,byte[] f,float x,float y,float u,float v,int ms){
        if(phase<0||phase>=4||f.length!=LENGTH||!coord(x)||!coord(y)||!coord(u)||!coord(v)||ms<40||ms>2000)throw new IllegalArgumentException();
        if(examples.size()>=MAX)throw new IllegalStateException("Perfil lleno");
        examples.add(new Example(phase,f,x,y,u,v,ms));
    }
    public boolean ready(){int mask=0;for(Example e:examples)mask|=1<<e.phase;return mask==15;}
    private static boolean equivalent(Example a,Example b){
        if(a==null||b==null||a.phase!=b.phase)return false;
        if(a.phase==3)return true;
        return Math.hypot(a.x-b.x,a.y-b.y)<.04&&Math.hypot(a.u-b.u,a.v-b.v)<.04
                &&Math.abs(a.millis-b.millis)<Math.max(180,a.millis*.32);
    }
    private static double score(Example e,byte[] f){
        int shift=0;
        for(int i=0;i<LENGTH;i++)shift+=(f[i]&255)-(e.visual[i]&255);
        int bias=Math.max(-8,Math.min(8,shift/LENGTH));
        int total=0,local=0,localCount=0;
        int cx=(int)(e.x*16),cy=(int)((e.y-1.0/7)*10.5);
        for(int i=0;i<LENGTH;i++){
            int delta=Math.abs((f[i]&255)-(e.visual[i]&255)-bias);
            total+=delta;int cell=i/3;
            if(e.phase!=3&&Math.abs(cell%16-cx)<=1&&Math.abs(cell/16-cy)<=1){local+=delta;localCount++;}
        }
        double global=total/(double)LENGTH+Math.abs(bias)*.35;
        return localCount==0?global:.78*global+.22*local/localCount;
    }
    public Example choose(int phase,byte[] f){
        Example best=null,other=null;double first=Double.POSITIVE_INFINITY,second=first;
        for(Example e:examples){
            if(e.phase!=phase&&e.phase!=phase+1&&!(phase==1&&e.phase==3))continue;
            double d=score(e,f);
            if(d<first){
                if(!equivalent(e,best)){other=best;second=first;}
                best=e;first=d;
            }else if(!equivalent(e,best)&&d<second){other=e;second=d;}
        }
        if(best==null||first>19||(other!=null&&second-first<2.6)){resetDecision();return null;}
        if(!equivalent(best,pending)){pending=best;votes=1;return null;}
        votes=Math.min(3,votes+1);
        return votes>=(best.phase==3?3:2)?best:null;
    }
    public void write(DataOutput d)throws IOException{
        d.writeInt(MAGIC);d.writeInt(examples.size());
        for(Example e:examples){d.writeByte(e.phase);d.write(e.visual);d.writeFloat(e.x);d.writeFloat(e.y);d.writeFloat(e.u);d.writeFloat(e.v);d.writeInt(e.millis);}
    }
    public static MatchBook read(DataInput d)throws IOException{
        if(d.readInt()!=MAGIC)throw new IOException("Formato desconocido");int n=d.readInt();
        if(n<0||n>MAX)throw new IOException("Conteo inválido");MatchBook b=new MatchBook();
        for(int i=0;i<n;i++){
            int phase=d.readUnsignedByte();byte[] f=new byte[LENGTH];d.readFully(f);
            try{b.add(phase,f,d.readFloat(),d.readFloat(),d.readFloat(),d.readFloat(),d.readInt());}
            catch(IllegalArgumentException ex){throw new IOException("Ejemplo inválido",ex);}
        }return b;
    }
}
