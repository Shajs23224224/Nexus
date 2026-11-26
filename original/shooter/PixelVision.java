package co.nexus.research;
import java.util.*;
/** Pixel-only adapter for the local OpenGL arena. No reference to ShooterWorld. */
public final class PixelVision {
 public static final int BARS=5;
 public static final float LEFT=.35f, RIGHT=.65f;
 public static float barY(int k){return .12f+.025f*k;}
 public static final class Frame {
  public final float[] luminance,bars,colors;
  Frame(float[] l,float[] b,float[] c){luminance=l;bars=b;colors=c;}
 }
 static float clip(float x){return Math.max(0,Math.min(1,x));}
 static int index(int w,int h,float x,float y){return 4*(Math.min(h-1,Math.max(0,(int)(y*h)))*w+Math.min(w-1,Math.max(0,(int)(x*w))));}
 static boolean filled(byte[] rgba,int i){return (rgba[i]&255)<90&&(rgba[i+1]&255)>180&&(rgba[i+2]&255)>160;}
 /** Input is bottom-up RGBA read from the rendered framebuffer. */
 public static Frame decode(byte[] rgba,int w,int h){if(w<16||h<16||rgba.length!=(long)w*h*4)throw new IllegalArgumentException("frame size");
  float[] l=new float[128],bars=new float[BARS],colors=new float[24];
  for(int j=0;j<8;j++)for(int i=0;i<16;i++){float r=0,g=0,b=0;for(int y=0;y<3;y++)for(int x=0;x<3;x++){int k=index(w,h,(i+(x+.5f)/3)/16,(7-j+(y+.5f)/3)/8);r+=(rgba[k]&255)/2295f;g+=(rgba[k+1]&255)/2295f;b+=(rgba[k+2]&255)/2295f;}l[j*16+i]=clip(.2126f*r+.7152f*g+.0722f*b);int region=(j/4)*4+i/4;colors[3*region]+=r/16;colors[3*region+1]+=g/16;colors[3*region+2]+=b/16;}
  for(int k=0;k<BARS;k++){int count=0;for(int i=0;i<32;i++)if(filled(rgba,index(w,h,LEFT+(RIGHT-LEFT)*(i+.5f)/32,barY(k)+.007f)))count++;bars[k]=count/32f;}
  return new Frame(l,bars,colors);
 }
 /** All action candidates are available: feasibility is not read from simulator internals. */
 public static CognitiveCore.Observation observe(Frame f,Frame prev,float[] predicted){float[] s=new float[128];s[0]=f.bars[0];s[1]=f.bars[1];s[2]=f.bars[2];s[8]=f.bars[3];s[11]=f.bars[4];float diff=0;for(int i=0;i<128;i++){if(prev!=null)diff+=Math.abs(f.luminance[i]-prev.luminance[i])/128;if(i%2==0)s[16+i/2]=clip((f.luminance[i]+f.luminance[i+1])*.5f);}s[3]=clip(diff*3);s[9]=0;s[13]=.5f;s[14]=.5f;
  if(predicted!=null){if(predicted.length!=16)throw new IllegalArgumentException("prediction size");for(int i=0;i<16;i++)s[80+i]=clip(predicted[i]);}
  for(int i=0;i<24;i++)s[96+i]=clip(f.colors[i]);s[120]=1;s[121]=prev==null?0:1;
  return new CognitiveCore.Observation(s,EnumSet.allOf(CognitiveCore.Action.class));
 }
}
