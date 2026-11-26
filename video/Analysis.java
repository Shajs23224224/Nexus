package co.nexus.video;
import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.*;

public class Analysis extends Service {
    volatile boolean cancelled;
    private Thread thread;
    NotificationManager manager;
    Notification note(String msg){
        Intent open=new Intent(this,Review.class);
        return new Notification.Builder(this,"analysis").setSmallIcon(android.R.drawable.ic_media_play)
          .setContentTitle("NEXUS Video").setContentText(msg).setOnlyAlertOnce(true)
          .setContentIntent(PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE)).build();
    }
    public android.os.IBinder onBind(Intent i){return null;}
    public int onStartCommand(Intent intent,int flags,int id){
        if(thread!=null && thread.isAlive())return START_NOT_STICKY;
        manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("analysis","Análisis de video",NotificationManager.IMPORTANCE_LOW));
        startForeground(15,note("Abriendo video"));
        Uri uri=intent.getData();cancelled=false;
        thread=new Thread(()->runVideo(uri,id));thread.start();return START_NOT_STICKY;
    }
    private void state(String msg){getSharedPreferences("video",0).edit().putString("status",msg).apply();}
    private void runVideo(Uri uri,int id){
        MediaMetadataRetriever reader=new MediaMetadataRetriever();
        File temp=null;
        try{
            reader.setDataSource(this,uri);
            int count=Integer.parseInt(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));
            if(count<1)throw new IOException("Video sin fotogramas");
            temp=File.createTempFile("track",".bin",getFilesDir());
            Region tracker=new Region();
            try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)))){
                out.writeInt(count);
                for(int i=0;i<count;i++){
                    if(cancelled)throw new IOException("Cancelado");
                    Bitmap frame=reader.getFrameAtIndex(i);
                    if(frame==null)throw new IOException("Fotograma ilegible: "+(i+1));
                    Bitmap small=Bitmap.createScaledBitmap(frame,Region.W,Region.H,true);
                    int[] regions=tracker.update(small);
                    if(small!=frame)small.recycle();frame.recycle();
                    out.writeByte(regions[0]);out.writeByte(regions[1]);
                    if(i%20==0){String msg="Fotograma "+(i+1)+" de "+count;state(msg);manager.notify(15,note(msg));}
                }
            }
            android.content.SharedPreferences prefs=getSharedPreferences("video",0);
            String old=prefs.getString("file","");
            if(!prefs.edit().putString("uri",uri.toString()).putString("file",temp.getName()).putString("status","Listo · "+count+" fotogramas").commit())throw new IOException("No se guardó el resultado");
            temp=null;
            if(!old.isEmpty())new File(getFilesDir(),old).delete();
        }catch(Exception e){state("Análisis interrumpido · "+e.getMessage());}
        finally{if(temp!=null)temp.delete();try{reader.release();}catch(Exception ignored){}stopSelfResult(id);}
    }
    public void onDestroy(){cancelled=true;super.onDestroy();}
}