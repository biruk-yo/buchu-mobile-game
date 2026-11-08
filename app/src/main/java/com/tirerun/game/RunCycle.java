package com.tirerun.game;

/** Smooth rear-view run poses with both hands guiding a tire directly ahead. */
public final class RunCycle {
    public static final float TAU=(float)(Math.PI*2);
    public static final float TIRE_SCALE=1.1f;
    public static final float TIRE_GROUND_Y=-60;
    // A mild rear-quarter projection reveals the wheel ahead of the torso.
    // Its sideways screen offset is smaller than its radius, not a side-by-side pose.
    public static final float FORWARD_VIEW_SKEW=.42f;
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
        // A short distance ahead on the ground, not above his head. The wheel
        // is naturally partly occluded by his body from the rear camera.
        tireX=-15-TIRE_GROUND_Y*FORWARD_VIEW_SKEW+sway;
        tireY=TIRE_GROUND_Y-46*TIRE_SCALE;
        // Canvas has a downward Y axis: a negative angle spins anticlockwise.
        tireAngle=-metres/.43f;
        leg(left,phase,-29+sway,-58+bob,-29,hop,-1);
        leg(right,cycle(phase+.5f),-4+sway,-58+bob,-3,hop,1);
        // Small coordinated palm strokes and elbow flexion make the forward
        // push visible while the rubber continues rotating under the hands.
        float contact=.95f+.018f*(float)Math.sin(a*2);
        float dx=27*TIRE_SCALE*contact;
        float dy=-46*TIRE_SCALE*(float)Math.sqrt(1-contact*contact);
        float push=(float)Math.cos(a*2)*2;
        pushingArm(leftArm,-35+sway,-132+bob,-32+sway,-106+bob+push,tireX-dx,tireY+dy);
        pushingArm(rightArm,5+sway,-132+bob,25+sway,-106+bob+push,tireX+dx,tireY+dy);
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
    private static void pushingArm(Arm out,float sx,float sy,float ex,float ey,float hx,float hy) {
        // Forearms point away from the camera and are foreshortened. Keeping
        // elbows below the shoulders avoids the previous overhead lifting pose.
        out.shoulderX=sx;out.shoulderY=sy;out.elbowX=ex;out.elbowY=ey;
        out.handX=hx;out.handY=hy;
    }
}
