package com.tirerun.game;

/** Checks the playable sightlines over a complete sequence of both-direction bends. */
public final class TrailCameraTest {
    private static void check(boolean b,String message) { if(!b)throw new AssertionError(message); }
    public static void main(String[] args) {
        TrailCamera c=new TrailCamera();
        float minYaw=10,maxYaw=-10;
        for(int distance=0;distance<4000;distance++) {
            c.update(distance,0,960,.05f,distance==0);
            minYaw=Math.min(minYaw,c.yaw);maxYaw=Math.max(maxYaw,c.yaw);
            float s=c.position;
            float left=c.screenX(s,-TrailCamera.LANE_WIDTH),right=c.screenX(s,TrailCamera.LANE_WIDTH);
            check(left>25 && right<405,"Player lanes remain visible around bends at "+distance);
            check(left<c.screenX(s,0) && c.screenX(s,0)<right,"Lane order never reverses");
            for(int lane=-1;lane<=1;lane++) {
                float lateral=lane*TrailCamera.LANE_WIDTH;
                float obstacle=c.screenX(s+10*TrailCamera.WORLD_SCALE,lateral);
                check(obstacle>0 && obstacle<432,"Approaching hazard stays visible at "+distance);
                check(c.screenY(s+8,lateral,0)<c.screenY(s,lateral,0),"Distant objects appear farther up the path");
                check(c.scale(s+8,lateral)<c.scale(s,lateral),"Distant objects shrink");
            }
        }
        check(minYaw<-.2f && maxYaw>.2f,"Route bends in both directions");
        c.update(500,0,960,.016f,true);
        float before=c.screenX(c.position,TrailCamera.LANE_WIDTH);
        for(int i=0;i<90;i++)c.update(500,1,960,.016f,false);
        check(c.screenX(c.position,TrailCamera.LANE_WIDTH)<before,"Camera follows a lane change");
        float pausedX=c.x,pausedYaw=c.yaw;
        c.update(500,1,960,0,false);
        check(c.x==pausedX && c.yaw==pausedYaw,"Paused camera stays still");
        float minElevation=100,maxElevation=-100,minSlope=1,maxSlope=-1;
        for(float screenHeight:new float[]{768,960})for(int distance=0;distance<4000;distance+=2) {
            c.update(distance,0,screenHeight,.05f,true);
            float s=c.position;
            minElevation=Math.min(minElevation,TrailCamera.elevation(s));
            maxElevation=Math.max(maxElevation,TrailCamera.elevation(s));
            minSlope=Math.min(minSlope,TrailCamera.slope(s));
            maxSlope=Math.max(maxSlope,TrailCamera.slope(s));
            check(Math.abs(c.screenY(s,0,0)-screenHeight*.82f)<.01f,"Hills keep the runner at a stable screen height");
            for(int lane=-1;lane<=1;lane++) {
                float lateral=lane*TrailCamera.LANE_WIDTH;
                float hazardS=s+25*TrailCamera.WORLD_SCALE;
                float hazardY=c.screenY(hazardS,lateral,0);
                check(hazardY>screenHeight*.3f && hazardY<screenHeight*.85f,"Hazards have usable vertical sightlines on hills");
                for(float near=s;near<hazardS-.5f;near+=.5f)
                    check(c.screenY(near,lateral,0)>hazardY,"A crest cannot hide a hazard within 25 metres");
                check(c.screenY(hazardS,lateral,1)<hazardY,"Raised objects stay above sloping ground");
                check(Math.abs(TrailCamera.ground(s,lateral)-TrailCamera.elevation(s))<.001f,"All three lanes share the road surface");
            }
            check(TrailCamera.ground(s,18)>TrailCamera.ground(s,7)+2,"Valley banks rise beyond village shoulders");
            float y=c.screenY(s+8,0,0),sky=c.skyline();
            c.update(distance+.02f,0,screenHeight,.001f,false);
            check(Math.abs(c.screenY(c.position+8,0,0)-y)<1 && Math.abs(c.skyline()-sky)<1,"Hills and skyline move continuously");
        }
        check(maxElevation-minElevation>10 && minSlope<-.2f && maxSlope>.2f,"Route includes substantial climbs and descents");
        System.out.println("PASS: winding-route and hill sightlines, stable runner height, valley banks, continuous camera, lane following and pause.");
    }
}
