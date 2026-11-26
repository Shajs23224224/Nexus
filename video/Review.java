package co.nexus.video;
import android.app.*;
import android.content.*;
import android.os.*;
import android.net.Uri;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

public class Review extends Activity {
    TextView status;ImageView image;SeekBar seek;
    SharedPreferences prefs;String file="";int index;volatile int token;
    ExecutorService worker=Executors.newSingleThreadExecutor();
    Handler main=new Handler(Looper.getMainLooper());
    Runnable poll=new Runnable(){public void run(){
        status.setText(prefs.getString("status","Selecciona una grabación"));
        String latest=prefs.getString("file","");
        if(!file.equals(latest)){file=latest;try(RandomAccessFile r=new RandomAccessFile(new File(getFilesDir(),file),"r")){
            seek.setMax(r.readInt()-1);seek.setProgress(0);display(0);
        }catch(IOException e){status.setText("Resultado ilegible");}}
        main.postDelayed(this,750);
    }};
    void display(int position){
        final int request=++token;
        final String name=file,uri=prefs.getString("uri","");
        if(name.isEmpty())return;
        worker.execute(()->{
            if(request!=token)return;
            MediaMetadataRetriever reader=new MediaMetadataRetriever();
            try(RandomAccessFile r=new RandomAccessFile(new File(getFilesDir(),name),"r")){
                r.seek(4L+2L*position);int observed=r.readByte(), predicted=r.readByte();
                reader.setDataSource(this,Uri.parse(uri));Bitmap src=reader.getFrameAtIndex(position);
                if(src==null)return;
                Bitmap img=src.copy(Bitmap.Config.ARGB_8888,true);src.recycle();
                Canvas c=new Canvas(img);Paint pen=new Paint();pen.setStyle(Paint.Style.STROKE);
                pen.setStrokeWidth(Math.max(3,img.getWidth()/250f));
                for(int j=0;j<2;j++){int cell=j==0?observed:predicted;if(cell<0)continue;
                    pen.setColor(j==0?Color.GREEN:Color.YELLOW);
                    float w=img.getWidth()/8f,h=img.getHeight()/6f,x=cell%8*w,y=cell/8*h,d=j*pen.getStrokeWidth();
                    c.drawRect(x+d,y+d,x+w-d,y+h-d,pen);}
                main.post(()->{if(request==token)image.setImageBitmap(img);else img.recycle();});
            }catch(Exception e){main.post(()->{if(request==token)status.setText("No se pudo abrir el fotograma");});}
            finally{try{reader.release();}catch(Exception ignored){}}
        });
    }
    public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("video",0);
        LinearLayout col=new LinearLayout(this);col.setOrientation(1);setContentView(col);
        Button choose=new Button(this);choose.setText("Analizar video");col.addView(choose);
        choose.setOnClickListener(v->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE),7));
        status=new TextView(this);col.addView(status);
        TextView legend=new TextView(this);legend.setText("Verde · movimiento observado     Amarillo · siguiente región estimada");col.addView(legend);
        image=new ImageView(this);col.addView(image,new LinearLayout.LayoutParams(-1,0,1));
        seek=new SeekBar(this);col.addView(seek);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean user){if(user)index=p;}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){display(s.getProgress());}});
    }
    protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);
        if(req==7 && result==RESULT_OK && data!=null && data.getData()!=null){
            Uri u=data.getData();getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startForegroundService(new Intent(this,Analysis.class).setData(u));}}
    protected void onResume(){super.onResume();poll.run();}
    protected void onPause(){main.removeCallbacks(poll);super.onPause();}
    protected void onDestroy(){token++;worker.shutdownNow();super.onDestroy();}
}