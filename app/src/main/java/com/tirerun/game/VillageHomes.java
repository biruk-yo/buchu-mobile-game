package com.tirerun.game;

import android.graphics.*;

/** Layered plaster, stone, timber and roof details for the village compounds. */
public final class VillageHomes {
    private final Paint p=new Paint(3);
    private final Path path=new Path();
    public void draw(Canvas c,float x,float y,float scale,int seed,float time,float night) {
        c.save();c.translate(x,y);c.scale(scale,scale);
        oval(c,-77,-9,82,17,0x3331432c);
        if(seed%3==0)tukul(c,seed,night,scale>.35f);
        else cottage(c,seed,time,night,scale>.35f);
        c.restore();
    }
    private void cottage(Canvas c,int seed,float time,float night,boolean detail) {
        if(seed%2==1)smoke(c,52,-166,time,seed);
        int plaster=seed%3==1?0xffe1d0ad:0xffd7d7c5;
        int shutter=seed%2==0?0xff416c76:0xff496954;
        // Separate front and side planes, with a stone foundation and deep eaves.
        poly(c,0xffa68c69,30,-81,68,-101,68,-19,30,0);
        poly(c,plaster,-54,-88,30,-81,30,0,-54,-7);
        poly(c,0xff8d806a,-54,-18,30,-12,30,0,-54,-7);
        poly(c,0xff6f705d,30,-12,68,-31,68,-19,30,0);
        poly(c,0x38523e2d,-54,-88,30,-81,30,-67,-54,-75);
        if(detail) {
            for(int i=0;i<13;i++) {
                float xx=-49+(i*29+seed*7)%73,yy=-24-(i*19+seed)%42;
                line(c,xx,yy,xx+2+(i%4),yy+.4f,0x257a7059,.6f);
            }
            for(int i=0;i<7;i++) {
                float xx=-53+i*12;
                line(c,xx,-16+(xx+54)*.07f,xx,-7+(xx+54)*.07f,0xffb9ae93,1);
            }
            line(c,-53,-12,29,-6,0xffb9ae93,1);
            line(c,-47,-44,-43,-37,0xffa99c82,.7f);line(c,-43,-37,-45,-30,0xffa99c82,.7f);
        }
        // A recessed open door, visible jamb, plank door and raised threshold.
        poly(c,0xff9e8662,-3,-67,27,-65,27,-1,-3,-3);
        poly(c,DayNightCycle.mix(0xff273c38,0xffffd282,night),0,-63,24,-61,24,-2,0,-4);
        poly(c,0xff766141,0,-63,5,-59,5,-4,0,-4);
        float open=7+2*(float)Math.sin(time*.35f+seed);
        poly(c,shutter,24,-61,24+open,-57,24+open,-1,24,-2);
        line(c,26,-54,26,-6,0x55ded6ab,1);oval(c,28,-28,30,-26,0xffdfc780);
        poly(c,0xffc8b895,-8,-3,29,-1,36,7,-12,5);
        poly(c,0xff8e8268,-12,5,36,7,36,11,-12,9);
        window(c,-43,-64,23,25,shutter,night);
        poly(c,0xff566c68,44,-77,59,-85,59,-53,44,-45);
        poly(c,DayNightCycle.mix(0xff283e43,0xffffdb8d,night),47,-73,57,-78,57,-56,47,-51);
        line(c,52,-76,52,-53,0xffd5c5a0,1.3f);
        // Corrugated roofs alternate muted zinc and weathered terracotta.
        int metal=seed%2==0?0xff7e9695:0xffa66b52;
        poly(c,0xffb4a58a,-67,-93,-15,-148,34,-83);
        if(detail) {
            for(int i=0;i<6;i++) {
                float f=(i+2)/8f;
                line(c,-15-52*f,-148+55*f,-15+49*f,-148+65*f,0xff9c8c70,.8f);
            }
            oval(c,-25,-125,-7,-107,0xff655f4c);
            for(int i=0;i<3;i++)line(c,-23,-120+i*4,-9,-120+i*4,0xffcbbd9d,1.5f);
        }
        line(c,-67,-93,-15,-148,0xff776f58,2);
        poly(c,metal,-15,-148,40,-158,82,-106,34,-83);
        poly(c,0xff4b5146,-67,-93,34,-83,82,-106,82,-100,34,-77,-67,-87);
        if(detail)for(int i=0;i<=13;i++) {
            float f=i/13f;
            line(c,-15+55*f,-148-10*f,34+48*f,-83-23*f,seed%2==0?0xffb8c5b9:0xffd29c77,1.2f);
            line(c,-15+55*f+2,-148-10*f,34+48*f+2,-83-23*f,0x4050453a,.7f);
        }
        line(c,-15,-148,40,-158,0xffcfbd99,3);
        line(c,-67,-91,34,-81,0xffa89974,2.5f);line(c,34,-81,82,-104,0xffc8b697,2);
        if(seed%2==1) {
            poly(c,0xffa78b70,46,-136,57,-140,57,-160,46,-157);
            poly(c,0xff796a57,57,-140,62,-143,62,-163,57,-160);
            poly(c,0xffc2b69a,43,-157,59,-163,64,-160,48,-154);
            line(c,48,-151,58,-154,0xff77634e,1);
        }
        // Lived-in details: water barrel, flower pot and a woven entrance mat.
        oval(c,-68,-26,-47,3,0xff5d7c7a);oval(c,-68,-29,-47,-21,0xff94aba1);
        line(c,-67,-11,-48,-9,0xff385751,2);line(c,-67,-22,-48,-20,0xff385751,2);
        poly(c,0xffbd8a53,1,12,29,13,32,22,-3,20);
        for(int i=0;i<4;i++)line(c,0,14+i*2,29,15+i*2,0xff6a6548,.8f);
        oval(c,37,-12,53,5,0xffaa6148);
        for(int i=0;i<4;i++) {
            line(c,45,-9,37+i*5,-26-(i%2)*5,0xff4f784a,2);
            oval(c,34+i*5,-31-(i%2)*5,41+i*5,-24-(i%2)*5,i%2==0?0xffe8ba5b:0xffc26758);
        }
        glow(c,night,4,25);
    }
    private void tukul(Canvas c,int seed,float night,boolean detail) {
        // Curved plaster catches the light in broad, readable bands.
        oval(c,-47,-88,47,-60,0xffc6a578);
        rect(c,-47,-74,47,-5,0xffc6a578);oval(c,-47,-20,47,7,0xffc6a578);
        poly(c,0xff9f805e,25,-75,47,-74,47,-5,34,5,24,6);
        poly(c,0xffe3c89c,-38,-73,-20,-77,-20,5,-38,0);
        oval(c,-47,-13,47,9,0xff857860);rect(c,-44,-19,44,-3,0xffb5a383);
        if(detail)for(int i=0;i<8;i++)line(c,-41+i*12,-16,-39+i*12,-2,0xff776c57,1);
        rect(c,5,-55,30,1,0xff876747);rect(c,8,-52,27,0,DayNightCycle.mix(0xff293d35,0xffffd58b,night));
        poly(c,0xff6b543b,8,-52,13,-48,13,0,8,0);
        window(c,-33,-56,21,24,0xff496b62,night);
        poly(c,0xff6a573c,-63,-70,0,-139,64,-70,41,-60,-37,-60);
        poly(c,0xffa38a52,-63,-70,0,-139,5,-133,-24,-65);
        poly(c,0xff877041,0,-139,64,-70,30,-63,7,-123);
        if(detail)for(int i=-12;i<=12;i++) {
            float end=i*4.8f;
            line(c,i*.18f,-133+Math.abs(i)*.25f,end,-67+(Math.abs(i)%3),i%3==0?0xffc6ae73:0xffaa915b,.8f);
            line(c,end*.46f,-101,end*.72f,-84,0x60685434,.7f);
        }
        line(c,-60,-67,0,-59,0xffcab17a,4);line(c,0,-59,61,-67,0xffb59961,4);
        oval(c,-4,-144,4,-133,0xff866d45);
        poly(c,0xffc5b18d,3,2,33,2,40,10,-3,10);
        glow(c,night,9,26);
    }
    private void window(Canvas c,float x,float y,float w,float h,int shutter,float night) {
        rect(c,x-3,y-3,x+w+3,y+h+3,0xff9a8460);
        rect(c,x,y,x+w,y+h,DayNightCycle.mix(0xff344e55,0xffffd68b,night));
        poly(c,0x55d5e4db,x+2,y+2,x+w-2,y+2,x+2,y+h-5);
        line(c,x+w*.5f,y,x+w*.5f,y+h,0xffd5c09a,1.5f);line(c,x,y+h*.5f,x+w,y+h*.5f,0xffd5c09a,1.5f);
        poly(c,shutter,x-3,y-2,x-11,y-5,x-11,y+h+2,x-3,y+h+1);
        poly(c,shutter,x+w+3,y-2,x+w+10,y-5,x+w+10,y+h+2,x+w+3,y+h+1);
        for(int i=0;i<4;i++) {
            line(c,x-10,y+2+i*6,x-4,y+4+i*6,0x60bfd0af,1);
            line(c,x+w+4,y+4+i*6,x+w+9,y+2+i*6,0x60bfd0af,1);
        }
        rect(c,x-5,y+h+1,x+w+5,y+h+5,0xffbca783);
    }
    private void glow(Canvas c,float night,float left,float right) {
        if(night<.01f)return;
        poly(c,((int)(night*60)<<24)|0xffc35b,left,0,right,0,right+24,30,left-24,27);
        poly(c,((int)(night*45)<<24)|0xffe3a1,left,0,right,0,right+8,16,left-8,15);
    }
    private void smoke(Canvas c,float x,float y,float time,int seed) {
        for(int i=0;i<5;i++) {
            float phase=RunCycle.cycle(time*.11f+i*.2f+seed*.13f);
            float dx=(float)Math.sin(phase*5+seed)*7+phase*17,yy=y-phase*57,r=3+phase*11;
            oval(c,x+dx-r,yy-r,x+dx+r,yy+r,((int)((1-phase)*42)<<24)|0xe7e3cf);
        }
    }
    private void rect(Canvas c,float l,float t,float r,float b,int col) {p.setColor(col);c.drawRect(l,t,r,b,p);}
    private void oval(Canvas c,float l,float t,float r,float b,int col) {p.setColor(col);c.drawOval(l,t,r,b,p);}
    private void line(Canvas c,float x,float y,float xx,float yy,int col,float w) {p.setColor(col);p.setStrokeWidth(w);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);}
    private void poly(Canvas c,int col,float... a) {path.reset();path.moveTo(a[0],a[1]);for(int i=2;i<a.length;i+=2)path.lineTo(a[i],a[i+1]);path.close();p.setColor(col);c.drawPath(path,p);}
}
