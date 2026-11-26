package co.nexus.research;
import java.util.*;
import java.io.*;
public class PixelTest {
 static int pass;static void check(boolean b,String s){if(!b)throw new AssertionError(s);pass++;System.out.println("PASS "+s);}
 static byte[] sample(float health,float ammo,float kits,float progress,float reserve,int tone){int w=320,h=180;byte[] pixels=new byte[w*h*4];for(int i=0;i<pixels.length;i+=4){pixels[i]=(byte)tone;pixels[i+1]=(byte)tone;pixels[i+2]=(byte)tone;pixels[i+3]=(byte)255;}float[] vals={health,ammo,kits,progress,reserve};for(int k=0;k<5;k++)for(int y=(int)(PixelVision.barY(k)*h);y<(int)(PixelVision.barY(k)*h)+Math.max(1,(int)(h*.015f));y++)for(int x=(int)(PixelVision.LEFT*w);x<(int)(PixelVision.RIGHT*w);x++){int i=4*(y*w+x);boolean on=x<(int)(PixelVision.LEFT*w)+(int)((PixelVision.RIGHT-PixelVision.LEFT)*w*vals[k]);pixels[i]=(byte)(on?26:9);pixels[i+1]=(byte)(on?242:11);pixels[i+2]=(byte)(on?230:15);}return pixels;}
 public static void main(String[] args)throws Exception{PixelVision.Frame a=PixelVision.decode(sample(1,.5f,.33f,0,.4f,60),320,180);PixelVision.Frame b=PixelVision.decode(sample(.5f,.4f,.33f,.33f,.4f,110),320,180);
 check(Math.abs(a.bars[0]-1)<.04&&Math.abs(a.bars[1]-.5f)<.04&&a.bars[3]==0,"visual HUD decoded from pixels");
 check(Math.abs(b.bars[0]-.5f)<.04&&b.luminance[0]>a.luminance[0],"pixel differences");SparseBrain brain=new SparseBrain(19);float[] p=brain.predictVideo(a.luminance,PixelVision.group(a));CognitiveCore.Observation o=PixelVision.observe(a,null,p);
 check(o.state[120]==1&&o.state[0]==a.bars[0]&&o.allowed.size()==16,"observation only uses rendered frame");check(o.state[80]==p[0],"forecast features enter decision model");
 float[] y=PixelVision.outcome(a,b,CognitiveCore.Action.TAG);check(y.length==16&&y[4]>0&&y[1]<1,"visual reward reflects HUD outcome");SparseBrain.Decision d=brain.choose(o,true);brain.learn(o,d.action,y);brain.learnVideo(a.luminance,b.luminance,PixelVision.group(a));check(brain.updates==1&&brain.videoPairs==1&&brain.trained()==4,"visual and decision networks learn in one model");
 long u=brain.updates,v=brain.videoPairs;brain.rankQuantized(PixelVision.observe(b,a,brain.predictVideo(b.luminance,PixelVision.group(b))));check(brain.updates==u&&brain.videoPairs==v,"visual evaluation frozen");
 ByteArrayOutputStream out=new ByteArrayOutputStream();brain.write(out);SparseBrain copy=SparseBrain.read(new ByteArrayInputStream(out.toByteArray()));check(copy.updates==1&&copy.videoPairs==1&&copy.trained()==4,"joint checkpoint");
 System.out.println("Pixel checks: "+pass);
 }
}
