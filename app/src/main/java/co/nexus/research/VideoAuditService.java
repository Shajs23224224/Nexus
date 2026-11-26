package co.nexus.research;
import android.app.*;
import android.content.*;
import android.os.*;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
/** User-initiated foreground video work. Own model and files, avoiding concurrent simulator writes. */
public final class VideoAuditService extends Service {
 public static final String STOP="co.nexus.research.STOP_VIDEO",CHANNEL="nx_video";private static final int ID=160;private final ExecutorService work=Executors.newSingleThreadExecutor();private volatile boolean active,busy;private NotificationManager nm;
 @Override public void onCreate(){super.onCreate();nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Análisis de video NEXUS",NotificationManager.IMPORTANCE_LOW));}
 Notification notification(String msg){Intent i=new Intent(this,VideoAuditService.class).setAction(STOP);PendingIntent stop=PendingIntent.getService(this,1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,SparseActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("NEXUS · Video").setContentText(msg).setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Detener",stop).build();}
 void update(String msg){getSharedPreferences("video16",MODE_PRIVATE).edit().putString("status",msg).apply();nm.notify(ID,notification(msg));}
 @Override public int onStartCommand(Intent intent,int flags,int startId){if(intent==null){stopSelf();return START_NOT_STICKY;}if(STOP.equals(intent.getAction())){active=false;return START_NOT_STICKY;}if(busy)return START_NOT_STICKY;Uri uri=intent.getData();if(uri==null){stopSelf();return START_NOT_STICKY;}busy=active=true;startForeground(ID,notification("Iniciando"));work.execute(()->runAudit(uri));return START_NOT_STICKY;}
 boolean cool(){PowerManager p=getSystemService(PowerManager.class);return Build.VERSION.SDK_INT<29||p.getCurrentThermalStatus()<PowerManager.THERMAL_STATUS_SEVERE;}
 void runAudit(Uri uri){AtomicFile model=new AtomicFile(new File(getFilesDir(),"video16.bin"));SparseBrain b;try(InputStream in=model.openRead()){b=SparseBrain.read(in);}catch(IOException e){b=new SparseBrain(15);}String result="";try{writeText(this,"video16-demo.txt","");update("Analizando fotogramas");VideoAudit.Result r=VideoAudit.audit(this,uri,b,()->active&&cool(),this::update);writeText(this,"video16-report.txt",r.report);writeText(this,"video16-demo.txt",r.demo);result=active&&cool()?"Análisis terminado":"Análisis parcial detenido";}catch(Exception e){result="Error: "+e.getMessage();try{writeText(this,"video16-report.txt",result);}catch(IOException ignored){}}
 FileOutputStream out=null;try{out=model.startWrite();b.write(out);model.finishWrite(out);result+=" | pesos guardados";}catch(IOException e){if(out!=null)model.failWrite(out);result+=" | error de guardado";}
 getSharedPreferences("video16",MODE_PRIVATE).edit().putString("status",result).putLong("pairs",b.videoPairs).apply();active=false;busy=false;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
 static void writeText(Context c,String name,String value)throws IOException{AtomicFile f=new AtomicFile(new File(c.getFilesDir(),name));FileOutputStream out=f.startWrite();try{out.write(value.getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(IOException e){f.failWrite(out);throw e;}}
 static String readText(Context c,String name)throws IOException{AtomicFile f=new AtomicFile(new File(c.getFilesDir(),name));try(InputStream in=f.openRead()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>262144)throw new IOException("Informe demasiado grande");out.write(buf,0,n);}return out.toString("UTF-8");}}
 @Override public void onTimeout(int startId,int fgsType){active=false;stopSelf();}
 @Override public void onDestroy(){active=false;work.shutdown();super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
