package com.tirerun.game;

import android.graphics.*;

/** Small roadside scenes in the same local metre/pixel scale as the village renderer. */
public final class VillageArt {
    public static final int STALL=9,GARDEN=10,HEDGE=11,HOUSEHOLD=12,LAUNDRY=13,NEIGHBOR=14,STONES=15;
    private final Paint p=new Paint(3);
    private final Path path=new Path();
    private final RectF bounds=new RectF();
    public void draw(Canvas c,int kind,float x,float y,float scale,int seed,float time) {
        c.save();c.translate(x,y);c.scale(scale,scale);
        switch(kind) {
            case STALL:stall(c,seed);break;
            case GARDEN:garden(c,seed);break;
            case HEDGE:hedge(c,seed);break;
            case HOUSEHOLD:household(c,seed);break;
            case LAUNDRY:laundry(c,seed,time);break;
            case NEIGHBOR:neighbor(c,seed,time);break;
            case STONES:stones(c,seed);break;
        }
        c.restore();
    }
    private void stall(Canvas c,int seed) {
        ellipse(c,-77,-8,80,14,0x303e422b);
        line(c,-58,0,-58,-115,0xff755234,5);line(c,58,0,58,-115,0xff755234,5);
        poly(c,0xff59422d,-62,-45,62,-45,56,-5,-55,-5);
        rect(c,-64,-48,64,-40,0xffb78a50);
        int cloth=seed%2==0?0xffa95237:0xff476b59;
        poly(c,cloth,-72,-98,-54,-126,54,-126,72,-98);
        for(int i=0;i<7;i++) {
            float a=-70+i*20;
            poly(c,i%2==0?0xffe1c78e:cloth,a,-98,a+20,-98,(a+20)*.76f,-126,a*.76f,-126);
        }
        for(int i=0;i<8;i++)ellipse(c,-72+i*18,-104,-54+i*18,-91,i%2==0?0xffe1c78e:cloth);
        line(c,-72,-98,72,-98,0xff765b38,2);
        for(int b=0;b<3;b++) {
            float x=-40+b*40;
            ellipse(c,x-17,-61,x+17,-39,0xffb48243);
            for(int i=0;i<7;i++) {
                float xx=x-11+(i%4)*7,yy=-57-(i/4)*7;
                ellipse(c,xx-4,yy-4,xx+4,yy+4,b==0?0xffb95332:b==1?0xff75914d:0xffd8b154);
            }
            line(c,x-16,-44,x+16,-44,0xff795630,2);
        }
        basket(c,-77,-2,1);basket(c,79,0,.85f);
    }
    private void garden(Canvas c,int seed) {
        ellipse(c,-67,-18,68,12,0xff8b7750);
        for(int row=0;row<3;row++) {
            float yy=-13+row*9;
            line(c,-56,yy+5,56,yy+5,0xffb4a373,2);
            for(int i=0;i<6;i++) {
                float xx=-50+i*20+(row%2)*4;
                line(c,xx,yy,xx,yy-18,0xff57764a,2);
                poly(c,0xff557846,xx,yy-4,xx-11,yy-15,xx-5,yy-16,xx,yy-9);
                poly(c,0xff819755,xx,yy-9,xx+9,yy-22,xx+11,yy-15,xx,yy-3);
                if(seed%3==0)ellipse(c,xx-3,yy-21,xx+3,yy-16,0xffd5b663);
            }
        }
    }
    private void hedge(Canvas c,int seed) {
        ellipse(c,-32,-3,34,8,0x25334427);
        for(int i=0;i<5;i++) {
            float xx=-25+i*12,yy=-10-(i%2)*8;
            ellipse(c,xx-13,yy-13,xx+13,yy+9,i%2==0?0xff517144:0xff66834c);
            if(seed%3==0) {
                ellipse(c,xx-4,yy-9,xx,yy-5,0xffe4bc5c);
                ellipse(c,xx+5,yy-4,xx+8,yy-1,0xffdba56b);
            }
        }
    }
    private void household(Canvas c,int seed) {
        int variant=seed%4;
        if(variant==0) { pot(c,-15,0,1);pot(c,18,0,.7f);basket(c,39,0,.6f); }
        else if(variant==1) {
            for(int row=0;row<3;row++)for(int i=0;i<3-row;i++) {
                float x=-21+i*19+row*10,y=-4-row*10;
                line(c,x-22,y-4,x+15,y-12,0xff785234,9);
                ellipse(c,x+10,y-16,x+20,y-7,0xffc39a62);
                ellipse(c,x+13,y-14,x+17,y-10,0xff91673f);
            }
        } else if(variant==2) {
            line(c,-35,-5,-35,-23,0xff715234,4);line(c,35,-5,35,-23,0xff715234,4);
            rect(c,-42,-27,42,-20,0xffb28a51);basket(c,-53,0,.7f);
            pot(c,52,0,.6f);
        } else {
            ellipse(c,-32,-23,32,5,0xff8c8970);ellipse(c,-32,-29,32,-7,0xffb4ac8a);
            ellipse(c,-23,-25,23,-10,0xff514f3c);
            for(int i=-2;i<=2;i++)line(c,i*12,-5,i*12,-14,0xff6c6d58,1.5f);
            line(c,-30,-8,-30,-66,0xff83613e,4);line(c,30,-8,30,-66,0xff83613e,4);
            line(c,-33,-65,33,-65,0xffa88451,5);line(c,0,-64,0,-24,0xffc7b381,1.5f);
            pot(c,45,0,.6f);
        }
    }
    private void basket(Canvas c,float x,float y,float s) {
        c.save();c.translate(x,y);c.scale(s,s);
        poly(c,0xffba9255,-15,-24,15,-24,11,0,-11,0);
        ellipse(c,-15,-28,15,-20,0xffdbc18b);ellipse(c,-11,-26,11,-22,0xff6b5736);
        for(int i=0;i<4;i++)line(c,-13,-18+i*5,13,-18+i*5,0xff8c6e40,1);
        for(int i=-2;i<=2;i++)line(c,i*5,-22,i*4,-1,0xffdfbc78,1);
        c.restore();
    }
    private void pot(Canvas c,float x,float y,float s) {
        c.save();c.translate(x,y);c.scale(s,s);
        ellipse(c,-15,-31,15,0,0xffa46740);ellipse(c,-12,-30,4,-6,0xffbc8451);
        rect(c,-6,-37,6,-25,0xffa46740);ellipse(c,-8,-40,8,-34,0xffc18b5c);
        ellipse(c,-5,-38,5,-35,0xff594431);line(c,-9,-16,8,-16,0xffd3a374,1.5f);
        c.restore();
    }
    private void laundry(Canvas c,int seed,float time) {
        line(c,-66,0,-66,-96,0xff735a3e,4);line(c,66,0,66,-96,0xff735a3e,4);
        line(c,-66,-90,0,-85,0xffc9bd92,1);line(c,0,-85,66,-90,0xffc9bd92,1);
        for(int i=0;i<4;i++) {
            float x=-54+i*30,y=-88+Math.abs(i-1.5f)*-1;
            float sway=(float)Math.sin(time*1.5f+seed+i)*2;
            int col=i%2==0?0xffece3c6:seed%2==0?0xff8b6575:0xff648990;
            poly(c,col,x,y,x+23,y,x+23+sway,y+42,x+sway,y+40);
            line(c,x+sway,y+34,x+23+sway,y+36,i%2==0?0xffb76443:0xffdfca92,3);
            line(c,x+3,y-2,x+3,y+3,0xff886139,2);line(c,x+20,y-2,x+20,y+3,0xff886139,2);
        }
    }
    private void neighbor(Canvas c,int seed,float time) {
        float wave=(float)Math.sin(time*1.7f+seed)*3;
        ellipse(c,-17,-3,18,4,0x353e432b);
        int skin=seed%2==0?0xff815035:0xff9b6745;
        line(c,-7,-42,-8,-5,skin,7);line(c,8,-42,9,-5,skin,7);
        line(c,-10,-2,-4,-2,0xff3a4536,6);line(c,6,-2,13,-2,0xff3a4536,6);
        int shirt=seed%3==0?0xffd4b65c:0xffefe5cb;
        poly(c,seed%2==0?0xff5a6b71:0xff856278,-15,-59,15,-59,seed%2==0?13:23,-20,-18,-20);
        poly(c,shirt,-14,-100,14,-100,18,-49,-18,-49);
        line(c,-17,-56,17,-56,0xff526d4b,3);line(c,-17,-51,17,-51,0xffb96945,3);
        line(c,-14,-94,-21,-69,shirt,8);line(c,-21,-69,-18,-54,skin,6);
        line(c,14,-94,24,-84,shirt,8);line(c,24,-84,29,-103+wave,skin,6);
        ellipse(c,25,-110+wave,33,-101+wave,skin);
        rect(c,-5,-110,5,-98,skin);ellipse(c,-13,-133,13,-104,skin);
        ellipse(c,-14,-136,14,-117,0xff30342a);
        if(seed%2==1) {poly(c,0xffe5dcc0,-15,-128,0,-142,16,-128,14,-112,0,-124,-14,-112);}
        ellipse(c,-6,-120,-3,-117,0xff292f24);ellipse(c,4,-120,7,-117,0xff292f24);
    }
    private void stones(Canvas c,int seed) {
        for(int i=0;i<3;i++) {
            float x=(i-1)*8,y=(i%2)*3;
            poly(c,i%2==0?0xff989580:0xffaaa18a,x-6,y,x-4,y-7,x+2,y-9,x+6,y-3,x+4,y+2);
        }
        if(seed%4==0)line(c,16,1,18,-11,0xff7c9558,2);
    }
    private void rect(Canvas c,float l,float t,float r,float b,int color) {p.setColor(color);c.drawRect(l,t,r,b,p);}
    private void ellipse(Canvas c,float l,float t,float r,float b,int color) {p.setColor(color);bounds.set(l,t,r,b);c.drawOval(bounds,p);}
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float width) {p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);}
    private void poly(Canvas c,int color,float... points) {p.setColor(color);path.reset();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();c.drawPath(path,p);}
}
