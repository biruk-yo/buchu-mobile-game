package com.tirerun.game;

public final class RunCycleTest {
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    private static float distance(float x,float y,float xx,float yy) { return (float)Math.hypot(x-xx,y-yy); }
    public static void main(String[] args) {
        RunCycle pose=new RunCycle();
        boolean leftLift=false,rightLift=false,stance=false;
        for(int frame=0;frame<240;frame++) {
            pose.sample(frame/240f,frame/160f,0);
            for(RunCycle.Arm arm:new RunCycle.Arm[]{pose.leftArm,pose.rightArm}) {
                check(distance(arm.shoulderX,arm.shoulderY,arm.elbowX,arm.elbowY)<36,"Upper arm stays within reach");
                check(distance(arm.elbowX,arm.elbowY,arm.handX,arm.handY)<35,"Forward forearm is foreshortened");
                check(arm.elbowY>arm.shoulderY+20 && arm.handY>arm.shoulderY,"Hands and elbows stay below shoulders, not overhead");
                float ex=(arm.handX-pose.tireX)/(27*RunCycle.TIRE_SCALE);
                float ey=(arm.handY-pose.tireY)/(46*RunCycle.TIRE_SCALE);
                check(Math.abs(ex*ex+ey*ey-1)<.0001,"Both hands stay on the tire throughout the stride");
            }
            float forwardOffset=pose.tireX-(-15+pose.sway);
            check(forwardOffset>20 && forwardOffset<27*RunCycle.TIRE_SCALE,"Rear-quarter view reveals the tire while retaining overlap with the boy");
            check(Math.abs(pose.tireY+46*RunCycle.TIRE_SCALE-RunCycle.TIRE_GROUND_Y)<.001,"Tire bottom stays at its ground contact");
            check(pose.tireY-46*RunCycle.TIRE_SCALE > -170,"Tire top stays below his head");
            check(pose.leftArm.handX<pose.tireX && pose.rightArm.handX>pose.tireX,"Hands guide opposite sides of the tire");
            check(pose.left.footX+15<pose.right.footX,"Feet do not cross");
            check(pose.left.kneeX+15<pose.right.kneeX,"Knees do not cross");
            check(pose.left.footY<=1 && pose.right.footY<=1,"Feet do not sink below ground");
            leftLift|=pose.left.lift>25;rightLift|=pose.right.lift>25;
            stance|=pose.left.lift==0 && pose.right.lift>20;
        }
        check(leftLift && rightLift && stance,"Clear alternating contact and heel lift");
        pose.sample(.9999f,3,0);float seam=pose.left.footY;
        pose.sample(.0001f,3,0);check(Math.abs(seam-pose.left.footY)<.1f,"Seam has no foot snap");
        for(int speed=10;speed<=100;speed++)check(RunCycle.cadence(speed)<=1.85f,"High speed cannot cause frantic cycling");
        float previous=0;
        for(int frame=0;frame<120;frame++) {
            pose.sample(frame/60f*RunCycle.cadence(23),frame/60f*7,0);
            if(frame>0)check(Math.abs(pose.left.footY-previous)<10,"No abrupt frame-to-frame foot jump");
            previous=pose.left.footY;
        }
        pose.sample(.4f,.4f,0);float angle=pose.tireAngle;
        pose.sample(.4f,1.9f,0);check(Math.abs(pose.tireAngle-angle+1.5f/.43f)<.001,"Tire spins anticlockwise with travel, independently of cadence");
        System.out.println("PASS: smooth foot loop, bounded cadence, leg separation, heel lift and two-hand contact with the tire ahead.");
    }
}
