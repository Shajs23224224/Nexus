package co.nexus.research;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.*;
import java.io.*;
/** Offline video analysis: OCR, generic labels, visual forecast error, optional local arena markers. */
public final class VideoAudit {
 public static final class Result{public final String report,demo;Result(String r,String d){report=r;demo=d;}}
 static final Pattern MARK=Pattern.compile("NXD\\s+(-?\\d+)\\s+(\\d+)\\s+([A-Z_]+)");
 static final class Markers {
  Long seed;boolean bad;final TreeMap<Integer,String> actions=new TreeMap<>();
  void add(String text){Matcher m=MARK.matcher(text);while(m.find())try{long s=Long.parseLong(m.group(1));int n=Integer.parseInt(m.group(2));String a=m.group(3);CognitiveCore.Action.valueOf(a);if(n<1||n>80)continue;if(seed==null)seed=s;if(seed!=s){bad=true;continue;}String old=actions.put(n,a);if(old!=null&&!old.equals(a))bad=true;}catch(RuntimeException ignored){}}
  String demo(){if(bad||seed==null||actions.isEmpty()||actions.firstKey()!=1||actions.lastKey()!=actions.size())return "";StringBuilder s=new StringBuilder("NXDEMO1\n"+seed+"\n");for(String a:actions.values())s.append(a).append('\n');return s.toString();}
 }
 public static Result audit(Context ctx,Uri uri,SparseBrain brain,BooleanSupplier active,Consumer<String> progress)throws IOException{
  MediaMetadataRetriever retriever=new MediaMetadataRetriever();TextRecognizer ocr=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);ImageLabeler label=ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);StringBuilder report=new StringBuilder("NEXUS 0.16 | Revisión local de video\nLos errores visuales no prueban errores del jugador. Etiquetas genéricas, no identificación de rivales.\n");Markers markers=new Markers();float[] previous=null;int pairs=0,held=0,decoded=0;double error=0;
  try{retriever.setDataSource(ctx,uri);long ms=Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));if(ms<=0)throw new IOException("Video vacío");int count=(int)Math.max(2,Math.min(96,ms/350+1));for(int i=0;i<count&&active.getAsBoolean();i++){long time=(long)(i*(ms-1d)/(count-1));Bitmap b=android.os.Build.VERSION.SDK_INT>=27?retriever.getScaledFrameAtTime(time*1000,MediaMetadataRetriever.OPTION_CLOSEST,960,540):retriever.getFrameAtTime(time*1000,MediaMetadataRetriever.OPTION_CLOSEST);if(b==null){previous=null;continue;}try{decoded++;float[] f=VideoStudy.features(b);if(previous!=null){int group=(pairs+held)%6;float[] p=brain.predictVideo(previous,group),target=SparseBrain.videoTarget(f);double e=0;for(int k=0;k<16;k++)e+=(p[k]-target[k])*(p[k]-target[k])/16;if((pairs+held)%5==4){error+=e;held++;}else{brain.learnVideo(previous,f,group);pairs++;}if(e>.10)report.append(time).append(" ms | cambio visual / error de predicción ").append(String.format(Locale.ROOT,"%.3f",e)).append('\n');}previous=f;
   // OCR every sample to collect markers; generic scene labels on every fourth sample.
   InputImage image=InputImage.fromBitmap(b,0);try{String t=Tasks.await(ocr.process(image),10,TimeUnit.SECONDS).getText();markers.add(t.replace('\n',' '));if(!t.trim().isEmpty())report.append(time).append(" ms | texto: ").append(t.substring(0,Math.min(240,t.length())).replace('\n',' ')).append('\n');}catch(Exception e){report.append(time).append(" ms | OCR no disponible\n");}
   if(i%4==0)try{List<ImageLabel> labels=Tasks.await(label.process(image),10,TimeUnit.SECONDS);for(int k=0;k<Math.min(3,labels.size());k++){ImageLabel l=labels.get(k);report.append(time).append(" ms | etiqueta: ").append(l.getText()).append(" ").append(String.format(Locale.ROOT,"%.2f",l.getConfidence())).append('\n');}}catch(Exception e){report.append(time).append(" ms | etiquetas no disponibles\n");}
  }finally{b.recycle();}progress.accept("Video "+(i+1)+"/"+count+" | pares "+pairs);}
  if(decoded<2&&active.getAsBoolean())throw new IOException("No se pudieron extraer fotogramas");String demo=markers.demo();if(!demo.isEmpty())try{StrategyLab.Review review=StrategyLab.importDemo(new ByteArrayInputStream(demo.getBytes(java.nio.charset.StandardCharsets.UTF_8)),brain,false);report.append("\nMarcadores de arena verificados: ").append(review).append('\n');}catch(IOException e){demo="";}report.append("\nFotogramas: ").append(decoded).append(" | pares entrenados: ").append(pairs).append(" | evaluación: ").append(held).append(held>0?String.format(Locale.ROOT," | MSE %.5f",error/held):"").append("\n").append(demo.isEmpty()?"No hay demostración local completa verificable. No se inferirán acciones profesionales de imágenes.":"Demostración disponible para importar en el laboratorio.");return new Result(report.toString(),demo);
  }catch(RuntimeException e){throw new IOException("Error de lectura de video",e);}finally{ocr.close();label.close();retriever.release();}
 }
}
