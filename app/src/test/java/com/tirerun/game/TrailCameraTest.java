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
        System.out.println("PASS: winding-route sightlines, lane order, depth, camera following and pause.");
    }
}
