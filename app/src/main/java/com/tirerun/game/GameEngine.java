package com.tirerun.game;

import java.util.ArrayList;
import java.util.Random;

/** Platform-independent rules, measured in world metres and seconds. */
public final class GameEngine {
    public enum State { MENU, RUNNING, PAUSED, OVER }
    public enum Kind { COIN, ROCK, LOG, MUD }
    public static final class Item {
        public final Kind kind;
        public final int lane;
        public float z;
        public boolean resolved;
        public Item(Kind kind, int lane, float z) { this.kind=kind; this.lane=lane; this.z=z; }
    }
    public interface Events { void coin(); void stumble(); void end(String reason); }
    public final ArrayList<Item> items = new ArrayList<>();
    public State state=State.MENU;
    public int lane=0, coins=0, strikes=0;
    public float visualLane=0, distance=0, elapsed=0, jumpTime=0, recovery=0, flash=0;
    public String endReason="";
    private float nextRow=24;
    private final Random random;
    private final Events events;
    public GameEngine(long seed, Events events) { random=new Random(seed); this.events=events; }
    public void start() {
        state=State.RUNNING; lane=0; visualLane=0; coins=0; strikes=0;
        distance=0; elapsed=0; jumpTime=0; recovery=0; flash=0; nextRow=24; items.clear();
        for(int i=0;i<4;i++) items.add(new Item(Kind.COIN,0,15+i*5));
        // Opening hazards are deliberately spaced to teach lane changes.
        items.add(new Item(Kind.ROCK,0,46));
        items.add(new Item(Kind.COIN,-1,46));
        items.add(new Item(Kind.MUD,-1,70));
        items.add(new Item(Kind.COIN,1,70));
    }
    public float speed() { return Math.min(23f, 10f+distance/180f); }
    public float jumpHeight() { return jumpTime<=0?0:(float)Math.sin(Math.PI*(1-jumpTime/0.85f)); }
    public void move(int direction) { if(state==State.RUNNING) lane=Math.max(-1,Math.min(1,lane+direction)); }
    public void jump() { if(state==State.RUNNING && jumpTime<=0) jumpTime=0.85f; }
    public void pause() { if(state==State.RUNNING) state=State.PAUSED; }
    public void resume() { if(state==State.PAUSED) state=State.RUNNING; }
    public void update(float dt) {
        if(state!=State.RUNNING || dt<=0) return;
        dt=Math.min(dt,0.05f);
        elapsed+=dt;
        visualLane+=(lane-visualLane)*Math.min(1,dt*14);
        jumpTime=Math.max(0,jumpTime-dt); flash=Math.max(0,flash-dt);
        if(strikes>0) { recovery-=dt; if(recovery<=0) strikes=0; }
        float step=speed()*dt; distance+=step; nextRow-=step;
        if(nextRow<=0) { spawnRow(); nextRow=19+random.nextFloat()*9; }
        for(int i=items.size()-1;i>=0;i--) {
            Item item=items.get(i);
            float oldZ=item.z; item.z-=step;
            if(!item.resolved && oldZ>0 && item.z<=0) {
                item.resolved=true;
                if(Math.abs(visualLane-item.lane)<0.48f) {
                    if(item.kind==Kind.COIN) { coins++; if(events!=null)events.coin(); }
                    else if(item.kind==Kind.MUD) { finish("The tire got stuck in the mud."); return; }
                    else if(jumpHeight()<0.35f) {
                        strikes++; recovery=8; flash=0.45f;
                        if(strikes>=2) { finish("Two stumbles! The dog caught up."); return; }
                        if(events!=null)events.stumble();
                    }
                }
            }
            if(item.z < -12) items.remove(i);
        }
    }
    private void spawnRow() {
        int blocked=random.nextInt(3)-1;
        Kind kind=Kind.values()[1+random.nextInt(3)];
        items.add(new Item(kind,blocked,105));
        int safe=(blocked+1+random.nextInt(2)+1)%3-1;
        for(int i=0;i<3;i++) items.add(new Item(Kind.COIN,safe,105+i*4));
    }
    private void finish(String reason) { state=State.OVER; endReason=reason; if(events!=null)events.end(reason); }
}
