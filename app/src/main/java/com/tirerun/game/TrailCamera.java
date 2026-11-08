package com.tirerun.game;

/** A perspective chase camera over a continuous winding route. Units are metres. */
public final class TrailCamera {
    public static final float WORLD_SCALE=.32f, LANE_WIDTH=1.18f;
    public float position, yaw, laneFollow, x, z, height, horizon;
    public final float focal=360, back=3.8f;
    private boolean initialized;
    public static float center(float s) {
        return 5.2f*(float)Math.sin(s/13f)+3.8f*(float)Math.sin(s/27f+.8f);
    }
    public static float heading(float s) {
        return (float)Math.atan(5.2/13*Math.cos(s/13)+3.8/27*Math.cos(s/27+.8));
    }
    public void update(float metres,float lane,float screenHeight,float dt,boolean reset) {
        position=metres*WORLD_SCALE;
        float target=heading(position+1.8f);
        if(!initialized || reset) { yaw=target;laneFollow=lane*.36f;initialized=true; }
        else {
            float blend=1-(float)Math.exp(-5*dt);
            yaw+=(target-yaw)*blend;
            laneFollow+=(lane*.36f-laneFollow)*blend;
        }
        x=center(position)-(float)Math.sin(yaw)*back+(float)Math.cos(yaw)*laneFollow;
        z=position-(float)Math.cos(yaw)*back-(float)Math.sin(yaw)*laneFollow;
        horizon=screenHeight*.43f;
        height=screenHeight*.39f*back/focal;
    }
    public float routeX(float s,float lateral) { return center(s)+(float)Math.cos(heading(s))*lateral; }
    public float routeZ(float s,float lateral) { return s-(float)Math.sin(heading(s))*lateral; }
    public float depth(float s,float lateral) {
        float dx=routeX(s,lateral)-x,dz=routeZ(s,lateral)-z;
        return dx*(float)Math.sin(yaw)+dz*(float)Math.cos(yaw);
    }
    public float screenX(float s,float lateral) {
        float dx=routeX(s,lateral)-x,dz=routeZ(s,lateral)-z;
        return 216+focal*(dx*(float)Math.cos(yaw)-dz*(float)Math.sin(yaw))/Math.max(.3f,depth(s,lateral));
    }
    public float screenY(float s,float lateral,float altitude) {
        return horizon+focal*(height-altitude)/Math.max(.3f,depth(s,lateral));
    }
    public float scale(float s,float lateral) { return focal/Math.max(.3f,depth(s,lateral)); }
    public static String district(float metres) {
        int part=((int)(metres/180))%3;
        return part==0?"THE WOODLAND PATH":part==1?"BETWEEN THE COMPOUNDS":"THE EUCALYPTUS GROVE";
    }
}
