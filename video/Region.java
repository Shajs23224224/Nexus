package co.nexus.video;
import android.graphics.Bitmap;

final class Region {
    static final int W=96,H=54,C=8,R=6;
    int[] previous, current=new int[W*H];
    int last=-1, vx, vy;
    int[] update(Bitmap bitmap){
        bitmap.getPixels(current,0,W,0,0,W,H);
        int best=-1, largest=0,total=0;
        int[] energy=new int[C*R];
        for(int y=0;y<H;y++)for(int x=0;x<W;x++){
            int i=y*W+x,c=current[i];
            current[i]=((c>>16&255)*3+(c>>8&255)*6+(c&255))/10;
            if(previous!=null){
                int change=Math.abs(current[i]-previous[i]);
                if(change>16)energy[y*R/H*C+x*C/W]+=change-16;
            }
        }
        for(int e:energy)total+=e;
        for(int i=0;i<energy.length;i++){
            int score=energy[i];
            if(last>=0 && Math.abs(last%C-i%C)<=1 && Math.abs(last/C-i/C)<=1)score+=score/5;
            if(score>largest){largest=score;best=i;}
        }
        // Diffuse camera motion or cuts do not support a local prediction.
        if(largest<320 || largest*10<total)best=-1;
        int forecast=-1;
        if(last>=0 && best>=0){
            int dx=best%C-last%C,dy=best/C-last/C;
            if(Math.abs(dx)<=1 && Math.abs(dy)<=1){
                vx=dx;vy=dy;
                forecast=Math.max(0,Math.min(R-1,best/C+vy))*C+Math.max(0,Math.min(C-1,best%C+vx));
            }
        }
        last=best;
        int[] tmp=previous;previous=current;current=tmp==null?new int[W*H]:tmp;
        return new int[]{best,forecast};
    }
}