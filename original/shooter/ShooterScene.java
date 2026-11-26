package co.nexus.research;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.*;
/** Perspective camera and depth-tested geometry, using OpenGL ES 1.0. */
final class ShooterScene extends GLSurfaceView implements GLSurfaceView.Renderer {
 volatile ShooterWorld.Snapshot frame;
 private final FloatBuffer cube,ground,ring;private final ByteBuffer indices;
 ShooterScene(Context c){super(c);setEGLConfigChooser(8,8,8,0,16,0);cube=buf(new float[]{-.5f,0,-.5f,.5f,0,-.5f,.5f,1,-.5f,-.5f,1,-.5f,-.5f,0,.5f,.5f,0,.5f,.5f,1,.5f,-.5f,1,.5f});indices=ByteBuffer.allocateDirect(36);indices.put(new byte[]{0,1,2,0,2,3,4,6,5,4,7,6,0,4,5,0,5,1,3,2,6,3,6,7,0,3,7,0,7,4,1,5,6,1,6,2}).position(0);ground=buf(new float[]{0,0,0,20,0,0,0,0,20,20,0,20});float[] r=new float[64*3];for(int i=0;i<64;i++){r[i*3]=(float)Math.cos(i*Math.PI/32);r[i*3+1]=.025f;r[i*3+2]=(float)Math.sin(i*Math.PI/32);}ring=buf(r);setRenderer(this);setRenderMode(RENDERMODE_WHEN_DIRTY);}
 static FloatBuffer buf(float[] a){FloatBuffer f=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();f.put(a).position(0);return f;}
 void show(ShooterWorld.Snapshot s){frame=s;requestRender();}
 public void onSurfaceCreated(GL10 g,EGLConfig c){g.glClearColor(.30f,.49f,.68f,1);g.glEnable(GL10.GL_DEPTH_TEST);g.glEnableClientState(GL10.GL_VERTEX_ARRAY);}
 public void onSurfaceChanged(GL10 g,int w,int h){g.glViewport(0,0,w,h);g.glMatrixMode(GL10.GL_PROJECTION);g.glLoadIdentity();GLU.gluPerspective(g,65,Math.max(1,w)/(float)Math.max(1,h),.08f,60);g.glMatrixMode(GL10.GL_MODELVIEW);}
 void box(GL10 g,float x,float z,float y,float w,float d,float h,float r,float b,float c){g.glPushMatrix();g.glTranslatef(x,y,z);g.glScalef(w,h,d);g.glColor4f(r,b,c,1);cube.position(0);indices.position(0);g.glVertexPointer(3,GL10.GL_FLOAT,0,cube);g.glDrawElements(GL10.GL_TRIANGLES,36,GL10.GL_UNSIGNED_BYTE,indices);g.glPopMatrix();}
 public void onDrawFrame(GL10 g){g.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);ShooterWorld.Snapshot s=frame;if(s==null)return;g.glLoadIdentity();float cp=(float)Math.cos(s.pitch);GLU.gluLookAt(g,s.x,1.55f,s.z,s.x+(float)Math.sin(s.yaw)*cp,1.55f+(float)Math.sin(s.pitch),s.z+(float)Math.cos(s.yaw)*cp,0,1,0);
  g.glColor4f(.20f,.32f,.25f,1);ground.position(0);g.glVertexPointer(3,GL10.GL_FLOAT,0,ground);g.glDrawArrays(GL10.GL_TRIANGLE_STRIP,0,4);
  for(int i=1;i<20;i++){box(g,i,10,.01f,.024f,20,.012f,.26f,.40f,.31f);box(g,10,i,.01f,20,.024f,.012f,.26f,.40f,.31f);}
  for(ShooterWorld.Box w:s.walls){box(g,w.x,w.z,0,w.w,w.d,w.h,.38f,.43f,.49f);box(g,w.x,w.z,w.h-.08f,w.w+.04f,w.d+.04f,.10f,.60f,.67f,.74f);}
  box(g,0,10,0,.2f,20,2,.20f,.25f,.32f);box(g,20,10,0,.2f,20,2,.20f,.25f,.32f);box(g,10,0,0,20,.2f,2,.20f,.25f,.32f);box(g,10,20,0,20,.2f,2,.20f,.25f,.32f);
  for(float[] t:s.targets)if(t[2]>0){box(g,t[0],t[1],.12f,.78f,.60f,1.24f,.82f,.22f,.18f);box(g,t[0],t[1],1.37f,.46f,.45f,.42f,1,.65f,.18f);}
  if(s.ammoAvailable)box(g,s.ammoX,s.ammoZ,0,.60f,.60f,.45f,.95f,.75f,.12f);if(s.kitAvailable)box(g,s.kitX,s.kitZ,0,.60f,.60f,.45f,.15f,.90f,.55f);
  g.glPushMatrix();g.glTranslatef(10,0,10);g.glScalef(s.zone,1,s.zone);g.glColor4f(.35f,.75f,1,1);ring.position(0);g.glVertexPointer(3,GL10.GL_FLOAT,0,ring);g.glDrawArrays(GL10.GL_LINE_LOOP,0,64);g.glPopMatrix();
 }
}
