package co.nexus.research;
import java.util.*;
import java.io.*;
import static co.nexus.research.CognitiveCore.Action.*;
public class ShooterTest {
 static int pass;
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);System.out.println("PASS "+name);pass++;}
 public static void main(String[] args)throws Exception{
  ShooterWorld a=new ShooterWorld(11);ShooterWorld b=new ShooterWorld(11);
  for(int i=0;i<25;i++){CognitiveCore.Action x=i%3==0?MOVE:i%3==1?TRACK:OBSERVE;a.step(x);b.step(x);}
  check(a.x==b.x&&a.z==b.z&&a.health==b.health&&a.targets[0].x==b.targets[0].x,"deterministic local physics");
  a=new ShooterWorld(12);a.x=6;a.z=5.4f;a.yaw=0;for(int i=0;i<6;i++)a.step(MOVE);check(a.z<5.7f,"wall collision");
  a=new ShooterWorld(13);a.targets[0].x=2;a.targets[0].z=5;a.pitch=(float)Math.atan2(-.55,3);a.step(TAG);check(a.ammo==11&&a.hits==1&&a.targets[0].hp==2,"virtual target and ammunition");
  a.step(RECHARGE);check(a.ammo==12&&a.reserve==23,"reload inventory conservation");
  a.health=50;a.step(RECOVER);check(a.health==85&&a.kits==0,"medkit restores health");
  a.x=a.ammoX;a.z=a.ammoZ;a.step(GET_ENERGY);check(!a.ammoAvailable&&a.reserve==47,"resource collection");
  a.step(INTERACT);check(a.inventory,"inventory toggle");
  a.ammo=0;check(!a.observe().allowed.contains(TAG),"empty magazine mask");
  SparseBrain brain=new SparseBrain(18);a=new ShooterWorld(14);CognitiveCore.Observation o=a.observe();SparseBrain.Decision d=brain.choose(o,true);float[] y=a.step(d.action);brain.learn(o,d.action,y);check(brain.updates==1&&brain.trained()==2&&y.length==16,"3D experience trains expanded network");
  long u=brain.updates;SparseBrain.Decision e=brain.rankQuantized(a.observe()).get(0);a.step(e.action);check(brain.updates==u,"evaluation leaves weights unchanged");
  ByteArrayOutputStream out=new ByteArrayOutputStream();brain.write(out);SparseBrain copy=SparseBrain.read(new ByteArrayInputStream(out.toByteArray()));check(copy.updates==1&&copy.trained()==2,"3D checkpoint roundtrip");
  System.out.println("Shooter checks: "+pass);
 }
}
