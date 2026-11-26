package co.nexus.research;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static co.nexus.research.CognitiveCore.Action.*;
/** All controls affect only this process's local 3D world. */
public final class ShooterActivity extends Activity {
 static final int MANUAL=1,TRAIN=2,EVAL=3,TEACH=4;
 final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
 final Handler ui=new Handler(Looper.getMainLooper());final AtomicReference<CognitiveCore.Action> queued=new AtomicReference<>();
 volatile int mode;volatile long generation;volatile boolean sprint,fire;volatile float joyX,joyY;volatile boolean alive=true;
 ShooterWorld world;SparseBrain brain;AtomicFile checkpoint;ShooterScene scene;TextView hud,inventory,notice;String msg="";long evalRuns,evalWins;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);checkpoint=new AtomicFile(new File(getFilesDir(),"nx-shooter18.bin"));
 FrameLayout root=new FrameLayout(this);scene=new ShooterScene(this);root.addView(scene,new FrameLayout.LayoutParams(-1,-1));
 hud=label("");FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);root.addView(hud,hp);
 TextView cross=label("+");cross.setTextSize(28);cross.setGravity(Gravity.CENTER);cross.setBackgroundColor(0);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.CENTER);root.addView(cross,cp);
 inventory=label("");inventory.setVisibility(View.GONE);FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(180),-2,Gravity.CENTER);ip.topMargin=dp(40);root.addView(inventory,ip);
 LinearLayout left=new LinearLayout(this);left.setOrientation(1);Joystick joy=new Joystick();left.addView(joy,new LinearLayout.LayoutParams(dp(116),dp(116)));Button run=new Button(this);run.setText("Correr");hold(run,()->sprint=true,()->sprint=false);left.addView(run);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(130),-2,Gravity.BOTTOM|Gravity.LEFT);lp.setMargins(dp(8),0,0,dp(8));root.addView(left,lp);
 LinearLayout right=new LinearLayout(this);right.setOrientation(1);Button shoot=new Button(this);shoot.setText("Disparo");hold(shoot,()->fire=true,()->fire=false);right.addView(shoot);action(right,"Recargar",RECHARGE);action(right,"Botiquín",RECOVER);action(right,"Munición",GET_ENERGY);action(right,"Recoger kit",GET_KIT);action(right,"Inventario",INTERACT);FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(120),-2,Gravity.RIGHT|Gravity.BOTTOM);rp.setMargins(0,0,dp(6),dp(8));root.addView(right,rp);
 LinearLayout modes=new LinearLayout(this);modes.setOrientation(0);modeButton(modes,"Jugar",MANUAL);modeButton(modes,"IA",TRAIN);modeButton(modes,"Evaluar",EVAL);modeButton(modes,"Enseñar",TEACH);Button pause=new Button(this);pause.setText("Pausa");pause.setOnClickListener(v->stop());modes.addView(pause,new LinearLayout.LayoutParams(0,-2,1));FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);mp.topMargin=dp(64);root.addView(modes,mp);
 notice=label("Arrastra el escenario para mirar. IA y Enseñar actualizan pesos.");FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);root.addView(notice,np);
 scene.setOnTouchListener(new View.OnTouchListener(){float x,y;public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-x,dy=e.getY()-y;if(Math.max(Math.abs(dx),Math.abs(dy))>dp(5)){queued.set(Math.abs(dx)>Math.abs(dy)?(dx>0?READ:TRACK):(dy>0?PLAN:SCAN));x=e.getX();y=e.getY();}return true;}return true;}});
 setContentView(root);worker.execute(()->{try(InputStream in=checkpoint.openRead()){brain=SparseBrain.read(in);msg="Modelo 3D recuperado";}catch(IOException e){brain=new SparseBrain(18);msg="Modelo 3D inicial";}world=new ShooterWorld(1800);render(null);});}
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 TextView label(String t){TextView v=new TextView(this);v.setText(t);v.setTextColor(0xfff0ffff);v.setTextSize(13);v.setPadding(dp(5),dp(3),dp(5),dp(3));v.setBackgroundColor(0xb0112230);return v;}
 void action(LinearLayout l,String name,CognitiveCore.Action a){Button b=new Button(this);b.setText(name);b.setTextSize(12);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);b.setOnClickListener(v->queued.set(a));l.addView(b,new LinearLayout.LayoutParams(-1,dp(39)));}
 void modeButton(LinearLayout l,String name,int m){Button b=new Button(this);b.setText(name);b.setTextSize(12);b.setPadding(0,0,0,0);b.setOnClickListener(v->start(m));l.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
 void hold(View v,Runnable down,Runnable up){v.setOnTouchListener((a,e)->{switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:down.run();return true;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:up.run();return true;default:return true;}});}
 void start(int m){long g=++generation;mode=m;queued.set(null);worker.execute(()->{if(generation!=g||brain==null)return;world=new ShooterWorld((m==EVAL?90000+evalRuns:1800+brain.episodes));tick(g,m);});}
 CognitiveCore.Action manual(){CognitiveCore.Action a=queued.getAndSet(null);if(a!=null)return a;if(fire)return TAG;if(Math.abs(joyX)>Math.abs(joyY)&&Math.abs(joyX)>.25f)return joyX>0?EXIT:EXPLORE;if(Math.abs(joyY)>.25f)return joyY<0?(sprint?DRIVE:MOVE):SHELTER;return sprint?DRIVE:OBSERVE;}
 boolean resourceLimit(){PowerManager p=getSystemService(PowerManager.class);ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();getSystemService(ActivityManager.class).getMemoryInfo(m);return m.lowMemory||(Build.VERSION.SDK_INT>=29&&p.getCurrentThermalStatus()>=PowerManager.THERMAL_STATUS_SEVERE);}
 void tick(long g,int m){if(!alive||generation!=g||mode!=m)return;if(resourceLimit()){mode=0;save();msg="Pausa por memoria o temperatura";render(null);return;}
 if(world.done){if(m==MANUAL||m==TEACH){mode=0;save();render(null);return;}world=new ShooterWorld(m==EVAL?90000+evalRuns:1800+brain.episodes);}
 CognitiveCore.Observation o=world.observe();SparseBrain.Decision d=null;CognitiveCore.Action a;if(m==TRAIN||m==EVAL){d=m==TRAIN?brain.choose(o,true):brain.rankQuantized(o).get(0);a=d.action;}else{a=manual();if(!o.allowed.contains(a))a=OBSERVE;}
 float[] y=world.step(a);if(m==TRAIN||m==TEACH)brain.learn(o,a,y);
 if(world.done){if(m==TRAIN||m==TEACH){brain.episodes++;if(world.won)brain.wins++;save();}else if(m==EVAL){evalRuns++;if(world.won)evalWins++;}}
 render(d);if(alive&&generation==g&&mode==m)worker.schedule(()->tick(g,m),m==TRAIN?120:100,TimeUnit.MILLISECONDS);}
 void save(){if(brain==null)return;FileOutputStream out=null;try{out=checkpoint.startWrite();brain.write(out);checkpoint.finishWrite(out);msg="Pesos 3D guardados";}catch(IOException e){if(out!=null)checkpoint.failWrite(out);msg="Error guardando pesos";}}
 void stop(){mode=0;generation++;queued.set(null);fire=sprint=false;joyX=joyY=0;if(!worker.isShutdown())worker.execute(()->{save();if(world!=null)render(null);});}
 void render(SparseBrain.Decision d){ShooterWorld.Snapshot f=world.snapshot();String text="NEXUS · Arena 3D | "+(mode==TRAIN?"IA autónoma":mode==EVAL?"Evaluación":mode==TEACH?"Demostración":mode==MANUAL?"Manual":"Pausa")+"\nVida "+(int)f.health+"  |  Munición "+f.ammo+"/"+f.reserve+"  |  Kits "+f.kits+"  |  Blancos "+f.kills+"/3\nImpactos "+f.hits+"/"+f.shots+"  |  Paso "+f.steps+"/"+ShooterWorld.LIMIT+"  |  "+world.event;String note="Redes "+brain.trained()+"/412 · actualizaciones "+brain.updates+" · episodios "+brain.wins+"/"+brain.episodes+" · evaluación "+evalWins+"/"+evalRuns+"\n"+(d==null?"":("Rol "+(d.agent+1)+" | valor "+String.format(Locale.ROOT,"%.2f",d.score)+" | incertidumbre "+String.format(Locale.ROOT,"%.2f",d.uncertainty)+"\n"))+msg;
 ui.post(()->{if(!alive)return;scene.show(f);hud.setText(text);notice.setText(note);inventory.setVisibility(f.inventory?View.VISIBLE:View.GONE);inventory.setText("INVENTARIO\nCargador: "+f.ammo+"\nReserva: "+f.reserve+"\nBotiquines: "+f.kits+"\nPulsa Inventario para cerrar");});}
 @Override protected void onResume(){super.onResume();if(scene!=null)scene.onResume();}
 @Override protected void onPause(){stop();if(scene!=null)scene.onPause();super.onPause();}
 @Override protected void onDestroy(){alive=false;mode=0;generation++;worker.shutdown();super.onDestroy();}
 final class Joystick extends View{final Paint p=new Paint(3);Joystick(){super(ShooterActivity.this);setContentDescription("Joystick de movimiento");}protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.9f;p.setColor(0x88334d5c);c.drawCircle(x,y,r,p);p.setColor(0xbb72e8df);c.drawCircle(x+joyX*r*.55f,y+joyY*r*.55f,r*.32f,p);}public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){joyX=joyY=0;}else{float r=Math.min(getWidth(),getHeight())*.5f;joyX=ShooterWorld.clamp((e.getX()-getWidth()/2f)/r,-1,1);joyY=ShooterWorld.clamp((e.getY()-getHeight()/2f)/r,-1,1);}invalidate();return true;}}
}
