package com.tirerun.game;

/** A 90-second world clock; the caller advances it only while play/menu is active. */
public final class DayNightCycle {
    public static final float SECONDS=90;
    public float angle,daylight,night,twilight,sunX,sunHeight,moonX,moonHeight;
    public int skyTop,skyBottom,worldTint;
    public void sample(float seconds) {
        float phase=RunCycle.cycle(seconds/SECONDS+.18f);
        angle=phase*RunCycle.TAU;
        sunHeight=(float)Math.sin(angle);moonHeight=-sunHeight;
        sunX=.5f-.53f*(float)Math.cos(angle);moonX=1-sunX;
        daylight=smooth(-.22f,.22f,sunHeight);night=1-daylight;
        twilight=(1-smooth(.03f,.38f,Math.abs(sunHeight)));
        skyTop=mix(0xff101d3c,0xffcce2df,daylight);
        skyBottom=mix(0xff3c4966,0xfff4dfaf,daylight);
        skyBottom=mix(skyBottom,0xffdf8e68,twilight*.85f);
        skyTop=mix(skyTop,0xff8b7b9e,twilight*.3f);
        int alpha=(int)(night*120);
        worldTint=(alpha<<24)|0x101c3a;
    }
    private static float smooth(float a,float b,float v) {
        float t=Math.max(0,Math.min(1,(v-a)/(b-a)));return t*t*(3-2*t);
    }
    public static int mix(int a,int b,float t) {
        t=Math.max(0,Math.min(1,t));
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int blue=(int)((a&255)*(1-t)+(b&255)*t);
        return 0xff000000|(r<<16)|(g<<8)|blue;
    }
}
