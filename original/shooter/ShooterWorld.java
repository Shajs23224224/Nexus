package co.nexus.research;
import java.util.*;
import static co.nexus.research.CognitiveCore.Action.*;
/** Local fictional 3D training arena. The same discrete controls drive human and AI. */
public final class ShooterWorld {
 public static final float SIZE=20f;public static final int LIMIT=420,MAX_TARGETS=3,MAG=12;
 public static final String[] CONTROL={"Esperar","Mira arriba","Desplazar izquierda","Avanzar","Recoger munición","Recoger botiquín","Curar","Retroceder","Girar izquierda","Disparo virtual","Recargar","Correr","Desplazar derecha","Girar derecha","Inventario","Mira abajo"};
 public static final class Box {public final float x,z,w,d,h;Box(float x,float z,float w,float d,float h){this.x=x;this.z=z;this.w=w;this.d=d;this.h=h;}}
 public static final class Target {float x,z,phase;int hp=3;Target(float x,float z,float phase){this.x=x;this.z=z;this.phase=phase;}}
 public static final class Snapshot {
  public final float x,z,yaw,pitch,health,zone,ammoX,ammoZ,kitX,kitZ;public final int ammo,reserve,kits,steps,hits,shots,kills;public final boolean done,won,ammoAvailable,kitAvailable,inventory;public final Box[] walls;public final float[][] targets;
  Snapshot(ShooterWorld a){x=a.x;z=a.z;yaw=a.yaw;pitch=a.pitch;health=a.health;zone=a.zone;ammo=a.ammo;reserve=a.reserve;kits=a.kits;steps=a.steps;hits=a.hits;shots=a.shots;kills=a.kills;done=a.done;won=a.won;ammoX=a.ammoX;ammoZ=a.ammoZ;kitX=a.kitX;kitZ=a.kitZ;ammoAvailable=a.ammoAvailable;kitAvailable=a.kitAvailable;inventory=a.inventory;walls=a.walls.clone();targets=new float[a.targets.length][3];for(int i=0;i<a.targets.length;i++){Target t=a.targets[i];targets[i]=new float[]{t.x,t.z,t.hp};}}
 }
 public final long seed;final Random random;final Box[] walls={new Box(6,8,2,4,2.4f),new Box(13,11,3,2,2.5f),new Box(9,15,2,2,2.1f),new Box(15,5,2,2,2.4f)};
 final Target[] targets;float x=2,z=2,yaw=0,pitch=0,health=100,zone=14;
 final float ammoX=3,ammoZ=8,kitX=17,kitZ=9;int ammo=MAG,reserve=24,kits=1,steps,hits,shots,kills;boolean done,won,ammoAvailable=true,kitAvailable=true,inventory;
 final boolean[] visited=new boolean[64];String event="Arena 3D";
 public ShooterWorld(long seed){this.seed=seed;random=new Random(seed);targets=new Target[]{new Target(4,13,random.nextFloat()*6.28f),new Target(12,5,random.nextFloat()*6.28f),new Target(17,16,random.nextFloat()*6.28f)};visited[cell()]=true;}
 public synchronized Snapshot snapshot(){return new Snapshot(this);}
 static float clamp(float f,float lo,float hi){return Math.max(lo,Math.min(hi,f));}
 static float unit(float x){return clamp(x,0,1);}
 float dist(float a,float b){return (float)Math.hypot(x-a,z-b);}
 int cell(){return Math.min(7,(int)(x/SIZE*8))+8*Math.min(7,(int)(z/SIZE*8));}
 boolean clear(float px,float pz){if(px<.45f||pz<.45f||px>SIZE-.45f||pz>SIZE-.45f)return false;for(Box w:walls)if(Math.abs(px-w.x)<w.w/2+.32f&&Math.abs(pz-w.z)<w.d/2+.32f)return false;return true;}
 void move(float dx,float dz){if(clear(x+dx,z))x+=dx;if(clear(x,z+dz))z+=dz;}
 boolean line(float ax,float az,float bx,float bz){float d=(float)Math.hypot(bx-ax,bz-az);int n=Math.max(1,(int)(d/.2f));for(int i=1;i<n;i++){float u=i/(float)n,px=ax+(bx-ax)*u,pz=az+(bz-az)*u;for(Box w:walls)if(Math.abs(px-w.x)<w.w/2&&Math.abs(pz-w.z)<w.d/2)return false;}return true;}
 float relative(Target t){float a=(float)Math.atan2(t.x-x,t.z-z)-yaw;return (float)Math.atan2(Math.sin(a),Math.cos(a));}
 Target nearest(){Target best=null;float d=Float.MAX_VALUE;for(Target t:targets)if(t.hp>0&&line(x,z,t.x,t.z)&&dist(t.x,t.z)<d){best=t;d=dist(t.x,t.z);}return best;}
 Target aimed(){Target best=null;float d=Float.MAX_VALUE;for(Target t:targets)if(t.hp>0&&line(x,z,t.x,t.z)){float range=dist(t.x,t.z);float elev=(float)Math.atan2(1.0f-1.55f,Math.max(.1f,range));if(range<18&&Math.abs(relative(t))<Math.atan2(.50f,range)&&Math.abs(pitch-elev)<Math.atan2(.8f,range)&&range<d){best=t;d=range;}}return best;}
 public synchronized CognitiveCore.Observation observe(){float[] s=new float[128];s[0]=health/100;s[1]=ammo/(float)MAG;s[2]=Math.min(1,kits/3f);Target t=nearest();s[3]=t==null?0:unit(1-dist(t.x,t.z)/18);s[4]=aimed()!=null?1:0;s[5]=ammoAvailable&&dist(ammoX,ammoZ)<1.5?1:0;s[6]=kitAvailable&&dist(kitX,kitZ)<1.5?1:0;s[7]=t==null?0:1;s[8]=kills/(float)MAX_TARGETS;s[9]=steps/(float)LIMIT;s[10]=0;s[11]=reserve/60f;s[12]=inventory?1:0;s[13]=x/SIZE;s[14]=z/SIZE;s[15]=unit((pitch+.6f)/1.2f);for(int i=0;i<64;i++)s[16+i]=visited[i]?1:0;
  if(t!=null){s[80]=unit((relative(t)+(float)Math.PI)/(2*(float)Math.PI));s[81]=unit(dist(t.x,t.z)/28.3f);}s[82]=unit((float)(Math.sin(yaw)*.5+.5));s[83]=unit((float)(Math.cos(yaw)*.5+.5));s[84]=unit(dist(ammoX,ammoZ)/28.3f);s[85]=unit(dist(kitX,kitZ)/28.3f);s[86]=unit(zone/14);s[87]=ammoAvailable?1:0;s[88]=kitAvailable?1:0;
  for(int i=0;i<24;i++){float ang=yaw+(float)(i*2*Math.PI/24);float d=.4f;while(d<8&&clear(x+(float)Math.sin(ang)*d,z+(float)Math.cos(ang)*d))d+=.4f;s[96+i]=unit(d/8);}
  EnumSet<CognitiveCore.Action> a=EnumSet.of(OBSERVE,SCAN,EXPLORE,MOVE,SHELTER,TRACK,DRIVE,EXIT,READ,INTERACT,PLAN);if(ammo>0)a.add(TAG);if(ammo<MAG&&reserve>0)a.add(RECHARGE);if(kits>0&&health<99)a.add(RECOVER);if(s[5]>0)a.add(GET_ENERGY);if(s[6]>0)a.add(GET_KIT);return new CognitiveCore.Observation(s,a);}
 public synchronized float[] step(CognitiveCore.Action a){if(done)throw new IllegalStateException("episode ended");if(!observe().allowed.contains(a))a=OBSERVE;float oldHp=health,oldX=x,oldZ=z,oldDist=nearest()==null?20:dist(nearest().x,nearest().z);int oldKills=kills,oldHits=hits;float u=.22f,cost=.03f,info=.05f;float fx=(float)Math.sin(yaw),fz=(float)Math.cos(yaw);switch(a){
  case MOVE:move(fx*.42f,fz*.42f);break;case DRIVE:move(fx*.76f,fz*.76f);cost=.10f;break;case SHELTER:move(-fx*.38f,-fz*.38f);break;case EXPLORE:move(fz*.38f,-fx*.38f);break;case EXIT:move(-fz*.38f,fx*.38f);break;
  case TRACK:yaw+=.09f;break;case READ:yaw-=.09f;break;case SCAN:pitch=clamp(pitch+.045f,-.6f,.6f);break;case PLAN:pitch=clamp(pitch-.045f,-.6f,.6f);break;
  case TAG:ammo--;shots++;cost=.2f;Target t=aimed();if(t!=null){hits++;t.hp--;if(t.hp==0)kills++;u=.85f;}else u=.04f;break;
  case RECHARGE:int count=Math.min(MAG-ammo,reserve);ammo+=count;reserve-=count;u=.65f;break;case RECOVER:kits--;health=Math.min(100,health+35);u=.78f;cost=.15f;break;case GET_ENERGY:ammoAvailable=false;reserve=Math.min(60,reserve+24);u=.86f;break;case GET_KIT:kitAvailable=false;kits=Math.min(3,kits+2);u=.86f;break;case INTERACT:inventory=!inventory;info=.2f;break;default:break;
 }
 yaw=(float)Math.atan2(Math.sin(yaw),Math.cos(yaw));steps++;zone=Math.max(5,14-steps*.022f);if(Math.hypot(x-10,z-10)>zone)health=Math.max(0,health-.7f);
 for(Target t:targets)if(t.hp>0){float nx=t.x+(float)Math.sin(t.phase+steps*.04f)*.065f,nz=t.z+(float)Math.cos(t.phase+steps*.04f)*.045f;if(clear(nx,nz)&&Math.hypot(nx-x,nz-z)>1){t.x=nx;t.z=nz;}if(steps%10==0&&dist(t.x,t.z)<7&&line(x,z,t.x,t.z))health=Math.max(0,health-4);}
 boolean novel=!visited[cell()];if(novel){visited[cell()]=true;info=.8f;u=Math.max(u,.5f);}Target n=nearest();if(n!=null&&dist(n.x,n.z)<oldDist-.1f)u=Math.max(u,.42f);if(Math.hypot(x-oldX,z-oldZ)>.05)u=Math.max(u,.32f);if(a==TRACK||a==READ||a==SCAN||a==PLAN){if(aimed()!=null)u=Math.max(u,.65f);}
 won=kills==MAX_TARGETS;done=won||health<=0||steps>=LIMIT;if(won)u=1;if(health<=0)u=0;event=CONTROL[a.ordinal()]+(hits>oldHits?" · impacto":"")+(kills>oldKills?" · objetivo":"")+(done?(won?" · victoria":" · fin"):"");
 return new float[]{u,unit(health/100),cost,info,kills/(float)MAX_TARGETS,ammo/(float)MAG,kits/3f,a==DRIVE?1:0,aimed()!=null?1:0,inventory?1:0,hits>oldHits?1:0,unit(1-(oldHp-health)/10),won?1:0,health>0?1:0,unit(1-steps/(float)LIMIT),novel?1:0};}
}
