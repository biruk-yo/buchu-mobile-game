package com.tirerun.game;

import android.graphics.*;

/** Small roadside scenes in the same local metre/pixel scale as the village renderer. */
public final class VillageArt {
    public static final int STALL=9,GARDEN=10,HEDGE=11,HOUSEHOLD=12,LAUNDRY=13,NEIGHBOR=14,STONES=15,CHICKENS=16;
    private final VillageHomes homes=new VillageHomes();
    private final Paint p=new Paint(3);
    private final Path path=new Path();
    private final RectF bounds=new RectF();
    public void draw(Canvas c,int kind,float x,float y,float scale,int seed,float time) {
        c.save();c.translate(x,y);c.scale(scale,scale);
        switch(kind) {
            case STALL:stall(c,seed,time);break;
            case GARDEN:garden(c,seed);break;
            case HEDGE:hedge(c,seed);break;
            case HOUSEHOLD:household(c,seed);break;
            case LAUNDRY:laundry(c,seed,time);break;
            case NEIGHBOR:neighbor(c,seed,time);break;
            case STONES:stones(c,seed);break;
            case CHICKENS:
                for(int i=0;i<3;i++) {
                    c.save();c.translate((i-1)*22+(float)Math.sin(time*.7f+seed+i)*6,-i*5);
                    c.scale(.45f,.45f);chicken(c,time+i,seed%2==0?1:-1,true);c.restore();
                }
                break;
        }
        c.restore();
    }
    public void house(Canvas c,float x,float y,float scale,int seed,float time,float night) {
        homes.draw(c,x,y,scale,seed,time,night);
    }
    private void stall(Canvas c,int seed,float time) {
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
        // A vendor sorts produce behind the counter.
        rect(c,-8,-73,12,-51,0xffe6d8b8);ellipse(c,-6,-91,10,-72,0xff925d3c);
        ellipse(c,-7,-94,11,-85,0xff29382d);
        float reach=(float)Math.sin(time*1.3f+seed)*8;
        line(c,10,-68,24+reach,-55,0xff925d3c,5);
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
        float wave=(float)Math.sin(time*1.7f+seed)*7;
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
        if(seed%3==1)basket(c,-25,-51,.7f);
    }
    public void surprise(Canvas c,GameEngine.Kind kind,float x,float y,float scale,float time,float travel,int direction) {
        c.save();c.translate(x,y);c.scale(scale,scale);
        if(kind==GameEngine.Kind.CHICKEN) {
            chicken(c,time,direction,false);
            c.save();c.translate(-direction*22,4);c.scale(.48f,.48f);chicken(c,time+.3f,direction,false);c.restore();
        } else if(kind==GameEngine.Kind.BASKET) {
            ellipse(c,-27,-3,29,9,0x403a392a);
            c.save();c.translate(0,-22-(float)Math.abs(Math.sin(travel*.7f))*3);c.rotate(travel*23*direction);
            ellipse(c,-24,-24,24,24,0xffb87938);ellipse(c,-20,-20,20,20,0xffd7af6a);
            ellipse(c,-15,-15,15,15,0xff6e4b2d);
            for(int i=0;i<12;i++) {
                float a=i*(float)Math.PI/6;
                line(c,16*(float)Math.cos(a),16*(float)Math.sin(a),22*(float)Math.cos(a),22*(float)Math.sin(a),0xfff1d39a,2);
            }
            line(c,-21,-8,21,-8,0xff9c713e,1.5f);line(c,-21,8,21,8,0xff9c713e,1.5f);
            c.restore();
        } else cart(c,travel,direction);
        c.restore();
    }
    private void chicken(Canvas c,float time,int direction,boolean peck) {
        c.save();c.scale(direction,1);
        ellipse(c,-22,-2,23,5,0x30344129);
        float step=(float)Math.sin(time*12)*7;
        line(c,-6,-13,-9+step,0,0xffd58e32,2.5f);line(c,6,-13,10-step,0,0xffd58e32,2.5f);
        line(c,-9+step,0,-4+step,1,0xffe7b047,2);line(c,10-step,0,15-step,1,0xffe7b047,2);
        poly(c,0xfff2eee0,-13,-15,-28,-32,-21,-34,-9,-25);
        ellipse(c,-21,-32,15,-8,0xfff5f0df);ellipse(c,-14,-28,8,-13,0xffd7d2c0);
        float bob=peck?Math.max(0,(float)Math.sin(time*2))*13:(float)Math.sin(time*12)*1.5f;
        ellipse(c,5,-43+bob,20,-24+bob,0xfffff6e1);
        ellipse(c,7,-48+bob,13,-39+bob,0xffc34f37);ellipse(c,12,-47+bob,18,-40+bob,0xffc34f37);
        poly(c,0xffe6ab3d,19,-36+bob,28,-32+bob,19,-30+bob);
        ellipse(c,13,-38+bob,16,-35+bob,0xff27352c);
        ellipse(c,16,-29+bob,21,-23+bob,0xffc34f37);
        if(!peck)line(c,-10,-23,2,-17+(float)Math.sin(time*12)*4,0xffaaa88e,2);
        c.restore();
    }
    private void cart(Canvas c,float travel,int direction) {
        ellipse(c,-43,-3,45,11,0x4538412c);
        // A tall, solid cart is visibly different from the low, hop-able basket.
        for(int side=-1;side<=1;side+=2) {
            c.save();c.translate(side*31,-12);c.rotate(travel*19*direction);
            ellipse(c,-12,-16,12,16,0xff293c40);ellipse(c,-8,-12,8,12,0xffa9b4aa);
            for(int i=0;i<4;i++) { c.rotate(45);line(c,0,-11,0,11,0xff4d625c,1.5f); }
            ellipse(c,-3,-3,3,3,0xffe3d5b3);c.restore();
        }
        poly(c,0xff496c71,-39,-50,32,-50,38,-19,-33,-19);
        poly(c,0xff315156,32,-50,45,-61,47,-31,38,-19);
        poly(c,0xffbfa573,-39,-50,-26,-63,45,-61,32,-50);
        for(int i=0;i<4;i++)line(c,-34,-44+i*7,33,-44+i*7,0xff9ba994,2);
        line(c,-39,-50,32,-50,0xffe2c88d,4);
        for(int i=0;i<6;i++) {
            float xx=-23+(i%3)*20,yy=-60-(i/3)*15;
            ellipse(c,xx-11,yy-13,xx+12,yy+3,i%2==0?0xffd4b56c:0xffa8b075);
            line(c,xx-8,yy-5,xx+8,yy-5,0xff81764e,1);
        }
        line(c,34,-32,61,-13,0xff786344,4);line(c,31,-25,55,-7,0xffab8e5d,3);
        poly(c,0xffdfb852,-22,-41,-14,-46,-6,-41,-14,-35);
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
