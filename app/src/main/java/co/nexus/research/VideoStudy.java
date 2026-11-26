package co.nexus.research;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.io.IOException;
/** Local, bounded, self-supervised learning from consecutive video frames. */
public final class VideoStudy {
 private VideoStudy(){}
 public static float[] features(Bitmap frame){Bitmap small=Bitmap.createScaledBitmap(frame,16,8,true);try{float[] v=new float[128];int[] p=new int[128];small.getPixels(p,0,16,0,0,16,8);for(int i=0;i<p.length;i++){int c=p[i];v[i]=(.2126f*((c>>16)&255)+.7152f*((c>>8)&255)+.0722f*(c&255))/255f;}return v;}finally{if(small!=frame)small.recycle();}}
 public static void study(Context context,Uri uri,SparseBrain brain,BooleanSupplier active,Consumer<String> status)throws IOException{
  MediaMetadataRetriever r=new MediaMetadataRetriever();int decoded=0,pairs=0,held=0;double error=0;float[] prev=null;
  try{r.setDataSource(context,uri);String duration=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(duration==null)throw new IOException("Duración desconocida");long ms=Long.parseLong(duration);if(ms<=0)throw new IOException("Video vacío");int samples=(int)Math.max(2,Math.min(96,ms/350+1));
   for(int i=0;i<samples&&active.getAsBoolean();i++){long us=(long)(i*(ms-1d)*1000/(samples-1));Bitmap frame=android.os.Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(us,MediaMetadataRetriever.OPTION_CLOSEST,160,90):r.getFrameAtTime(us,MediaMetadataRetriever.OPTION_CLOSEST);if(frame==null){prev=null;continue;}float[] f;try{f=features(frame);}finally{frame.recycle();}decoded++;if(prev!=null){int group=(pairs+held)%6;if((pairs+held)%5==4){float[] p=brain.predictVideo(prev,group),y=SparseBrain.videoTarget(f);for(int k=0;k<16;k++)error+=(p[k]-y[k])*(p[k]-y[k])/16;held++;}else{brain.learnVideo(prev,f,group);pairs++;}}prev=f;status.accept("Video: "+(i+1)+"/"+samples+" | pares entrenados "+pairs+" | validación "+held+(held>0?String.format(java.util.Locale.ROOT," | MSE %.4f",error/held):""));}
   if(decoded<2&&active.getAsBoolean())throw new IOException("No se pudieron extraer dos fotogramas");
  }catch(RuntimeException e){throw new IOException("No se pudo leer el video",e);}finally{r.release();}
 }
}
