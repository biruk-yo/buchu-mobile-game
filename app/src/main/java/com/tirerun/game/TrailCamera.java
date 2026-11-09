package com.tirerun.game;

/** A perspective chase camera over a continuous winding route. Units are metres. */
public final class TrailCamera {
    public static final float WORLD_SCALE=.32f, LANE_WIDTH=1.18f;
    public float position, yaw, laneFollow, x, z, height, horizon;
    private float groundHeight, grade;
    public final float focal=360, back=3.8f;
    private boolean initialized;
    public static float center(float s) {
        return 5.2f*(float)Math.sin(s/13f)+3.8f*(float)Math.sin(s/27f+.8f);
    }
    public static float heading(float s) {
        return (float)Math.atan(5.2/13*Math.cos(s/13)+3.8/27*Math.cos(s/27+.8));
    }
    public static float elevation(float s) {
        return 4.8f*(float)Math.sin(s/18f)+1.6f*(float)Math.sin(s/38f+.7f);
    }
    public static float slope(float s) {
        return (4.8f/18f)*(float)Math.cos(s/18f)+(1.6f/38f)*(float)Math.cos(s/38f+.7f);
    }
    public static float ground(float s,float lateral) {
        // Flat village shoulders open into rounded, grassy valley walls.
        float bank=Math.max(0,Math.abs(lateral)-7);
        return elevation(s)+(1-(float)Math.exp(-bank*bank/110f))
            *(4.8f+1.6f*(float)Math.sin(s/29f+(lateral<0?0:1.8f)));
    }
    public void update(float metres,float lane,float screenHeight,float dt,boolean reset) {
        position=metres*WORLD_SCALE;
        groundHeight=elevation(position);
        grade=slope(position);
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
        // Follow the local slope about the runner's feet, avoiding vertical bob
        // while retaining the visible rises and dips farther along the route.
        return horizon+focal*(height+groundHeight-ground(s,lateral)-altitude
            +grade*(s-position))/Math.max(.3f,depth(s,lateral));
    }
    public float skyline() { return horizon+focal*grade; }
    public float scale(float s,float lateral) { return focal/Math.max(.3f,depth(s,lateral)); }
    public static String district(float metres) {
        int part=((int)(metres/180))%3;
        return part==0?"THE WOODLAND PATH":part==1?"BETWEEN THE COMPOUNDS":"THE EUCALYPTUS GROVE";
    }
}
