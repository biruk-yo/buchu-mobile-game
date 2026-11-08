package com.tirerun.game;

/** Smooth rear-view run poses, bounded cadence, and fixed two-hand tire contact. */
public final class RunCycle {
    public static final float TAU=(float)(Math.PI*2);
    public static final class Leg {
        public float hipX,hipY,kneeX,kneeY,footX,footY,lift;
    }
    public static final class Arm { public float shoulderX,shoulderY,elbowX,elbowY,handX,handY; }
    public final Leg left=new Leg(),right=new Leg();
    public final Arm leftArm=new Arm(),rightArm=new Arm();
    public float phase,bob,sway,tireX,tireY,tireAngle;
    public static float cycle(float v) { return v-(float)Math.floor(v); }
    private static final float[] FOOT={1,1,-9,-28,-35,-22,-6,1};
    private static final float[] KNEE={-27,-25,-29,-37,-40,-35,-28,-26};
    public static float cadence(float speed) { return Math.max(1.35f,Math.min(1.85f,1.35f+(speed-10)*.038f)); }
    public void sample(float stridePhase,float metres,float hop) {
        phase=cycle(stridePhase);float a=phase*TAU;
        bob=-(1-(float)Math.cos(a*2))*.7f;sway=(float)Math.sin(a)*.8f;
        tireX=35+(float)Math.sin(a)*.65f;tireY=-66;
        tireAngle=metres/.43f;
        leg(left,phase,-29+sway,-58+bob,-29,hop,-1);
        leg(right,cycle(phase+.5f),-4+sway,-58+bob,-3,hop,1);
        // Both palms use exact points on the tire's outer ellipse throughout the gait.
        float dx=27*(float)Math.sqrt(.5),dy=-46*(float)Math.sqrt(.5);
        arm(leftArm,-35+sway,-132+bob,tireX-dx,tireY+dy,1);
        arm(rightArm,5+sway,-132+bob,tireX+dx,tireY+dy,-1);
    }
    private static float pose(float[] frames,float phase) {
        float at=cycle(phase)*frames.length;int i=(int)at;float t=at-i;
        float a=frames[(i+7)%8],b=frames[i%8],c=frames[(i+1)%8],d=frames[(i+2)%8];
        return .5f*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);
    }
    private static void leg(Leg out,float t,float hx,float hy,float base,float hop,int side) {
        out.hipX=hx;out.hipY=hy;
        out.footX=base+(float)Math.sin(t*TAU)*1.2f;
        out.footY=Math.min(1,pose(FOOT,t))-hop*8;
        out.kneeX=hx+side*1.2f;
        out.kneeY=pose(KNEE,t)+(hy+58)*.45f-hop*3;
        out.lift=Math.max(0,-out.footY);
    }
    private static void arm(Arm out,float sx,float sy,float hx,float hy,int bend) {
        out.shoulderX=sx;out.shoulderY=sy;out.handX=hx;out.handY=hy;
        float dx=hx-sx,dy=hy-sy,d=(float)Math.hypot(dx,dy);
        float a=(34*34-35*35+d*d)/(2*d);
        float h=(float)Math.sqrt(Math.max(0,34*34-a*a));
        out.elbowX=sx+a*dx/d-bend*h*dy/d;
        out.elbowY=sy+a*dy/d+bend*h*dx/d;
    }
}
