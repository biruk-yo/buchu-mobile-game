package com.tirerun.game;

import java.util.ArrayList;
import java.util.Random;

/** Platform-independent rules, measured in world metres and seconds. */
public final class GameEngine {
    // Catch up after slow frames in small steps, but bound work after a long stall.
    public static final float MAX_FRAME_SECONDS=.25f;
    public static final int MISSION_REWARD=25, NEAR_MISS_POINTS=25, PERFECT_HOP_POINTS=40;
    public enum State { MENU, RUNNING, PAUSED, OVER }
    public enum Kind { COIN, ROCK, LOG, MUD, CHICKEN, BASKET, CART }
    public static final class Item {
        public final Kind kind;
        public final int lane;
        public float z;
        public boolean resolved, collected, threatened, warned;
        public float warningAge;
        public Item(Kind kind, int lane, float z) { this.kind=kind; this.lane=lane; this.z=z; }
        public boolean surprise() { return kind==Kind.CHICKEN || kind==Kind.BASKET || kind==Kind.CART; }
        public float lateral() {
            if(!surprise())return lane;
            float side=lane==1?1:-1;
            float t=Math.max(0,Math.min(1,(75-z)/57));t=t*t*(3-2*t);
            float entry=side*2.85f+(lane-side*2.85f)*t;
            // Chickens continue across after passing the player; collision is
            // checked once at the clearly marked target lane.
            if(kind==Kind.CHICKEN && z<0)entry+=(Math.min(1,-z/12))*(-side*2.8f-lane);
            return entry;
        }
    }
    public interface Events {
        void coin(); void stumble(); void end(String reason);
        default void skill(String label,int points) { }
        default void missionComplete() { }
        default void warning(Kind kind) { }
    }
    public final ArrayList<Item> items = new ArrayList<>();
    public State state=State.MENU;
    public int lane=0, coins=0, strikes=0;
    public int bonusPoints,nearMisses,perfectHops,cleanHops;
    public boolean missionComplete;
    public float visualLane=0, distance=0, elapsed=0, jumpTime=0, recovery=0, flash=0;
    public String endReason="";
    private float nextRow=24;
    private float lastSteerTime=-10;
    private int missionNumber;
    private int rowNumber,surpriseNumber;
    private final Random random;
    private final Events events;
    public GameEngine(long seed, Events events) { random=new Random(seed); this.events=events; }
    public void start() { start(0); }
    public void start(int completedMissions) {
        state=State.RUNNING; lane=0; visualLane=0; coins=0; strikes=0;
        bonusPoints=nearMisses=perfectHops=cleanHops=0;lastSteerTime=-10;
        missionNumber=Math.max(0,completedMissions);missionComplete=false;
        rowNumber=surpriseNumber=0;
        distance=0; elapsed=0; jumpTime=0; recovery=0; flash=0; nextRow=24; items.clear();
        for(int i=0;i<4;i++) items.add(new Item(Kind.COIN,0,15+i*5));
        // Opening hazards are deliberately spaced to teach lane changes.
        items.add(new Item(Kind.ROCK,0,46));
        items.add(new Item(Kind.COIN,-1,46));
        items.add(new Item(Kind.MUD,-1,70));
        items.add(new Item(Kind.COIN,1,70));
    }
    // A brisk arcade start, building to full pace in roughly a minute of running.
    public float baseSpeed() { return Math.min(32f,18f+distance/100f); }
    public float downhillBoost() {
        float t=Math.max(0,Math.min(1,(-TrailCamera.slope(distance*TrailCamera.WORLD_SCALE)-.06f)/.2f));
        return t*t*(3-2*t);
    }
    public float speed() { return baseSpeed()+6*downhillBoost(); }
    public int score() { return (int)distance+bonusPoints; }
    public int earnedCoins() { return coins+(missionComplete?MISSION_REWARD:0); }
    public Item approachingSurprise() {
        Item nearest=null;
        for(Item item:items)if(item.surprise() && item.warned && !item.resolved && item.z>0
            && (nearest==null || item.z<nearest.z))nearest=item;
        return nearest;
    }
    public static String warningTitle(Kind kind) {
        return kind==Kind.CHICKEN?"CHICKENS CROSSING!":kind==Kind.BASKET?"ROLLING BASKET!":"CART ENTERING!";
    }
    public static String warningHint(Item item) {
        String lane=item.lane<0?"LEFT":item.lane>0?"RIGHT":"CENTER";
        return lane+" LANE  ·  "+(item.kind==Kind.CART?"SWERVE — DON'T HOP":"HOP OR SWERVE");
    }
    public static int missionTarget(int number) {
        int tier=Math.min(3,Math.max(0,number)/3);
        switch(Math.floorMod(number,3)) {
            case 0:return 12+4*tier;
            case 1:return 3+tier;
            default:return 350+100*tier;
        }
    }
    public static String missionLabel(int number) {
        int target=missionTarget(number);
        switch(Math.floorMod(number,3)) {
            case 0:return "Collect "+target+" coins in one run";
            case 1:return "Hop "+target+" obstacles without a stumble";
            default:return "Run "+target+" metres";
        }
    }
    public String missionLabel() { return missionLabel(missionNumber); }
    public int missionTarget() { return missionTarget(missionNumber); }
    public int missionProgress() {
        if(missionComplete)return missionTarget();
        int progress=missionNumber%3==0?coins:missionNumber%3==1?cleanHops:(int)distance;
        return Math.min(missionTarget(),progress);
    }
    public float jumpHeight() { return jumpTime<=0?0:(float)Math.sin(Math.PI*(1-jumpTime/0.85f)); }
    public void move(int direction) {
        if(state!=State.RUNNING)return;
        int next=Math.max(-1,Math.min(1,lane+direction));
        if(next!=lane) { lane=next;lastSteerTime=elapsed; }
    }
    public void jump() { if(state==State.RUNNING && jumpTime<=0) jumpTime=0.85f; }
    public void pause() { if(state==State.RUNNING) state=State.PAUSED; }
    public void resume() { if(state==State.PAUSED) state=State.RUNNING; }
    public void update(float dt) {
        if(state!=State.RUNNING || !Float.isFinite(dt) || dt<=0) return;
        float remaining=Math.min(dt,MAX_FRAME_SECONDS);
        while(remaining>0 && state==State.RUNNING) {
            float step=Math.min(remaining,.05f);
            updateStep(step);
            remaining-=step;
        }
    }
    private void updateStep(float dt) {
        float previousLane=visualLane,previousJump=jumpTime,previousTime=elapsed;
        elapsed+=dt;
        visualLane+=(lane-visualLane)*Math.min(1,dt*14);
        jumpTime=Math.max(0,jumpTime-dt); flash=Math.max(0,flash-dt);
        if(strikes>0) { recovery-=dt; if(recovery<=0) strikes=0; }
        float step=speed()*dt; distance+=step; nextRow-=step;
        checkMission();
        if(nextRow<=0) { spawnRow(); nextRow=19+random.nextFloat()*9; }
        for(int i=items.size()-1;i>=0;i--) {
            Item item=items.get(i);
            if(item.surprise() && !item.resolved) {
                if(!item.warned && item.z<=105) {
                    item.warned=true;item.warningAge=0;
                    if(events!=null)events.warning(item.kind);
                } else if(item.warned)item.warningAge+=dt;
            }
            float oldZ=item.z; item.z-=step;
            if(item.kind!=Kind.COIN && !item.resolved && oldZ>0 && oldZ<=speed()*.5f
                && Math.abs(previousLane-item.lane)<.48f)item.threatened=true;
            if(!item.resolved && oldZ>0 && item.z<=0) {
                item.resolved=true;
                // A malformed/late spawn must never create an unannounced hit.
                if(item.surprise() && item.warningAge<2)continue;
                // Judge timing at the actual crossing, including on slow frames.
                float crossing=Math.min(1,oldZ/step);
                float gap=Math.abs(previousLane+(visualLane-previousLane)*crossing-item.lane);
                float jumpRemaining=Math.max(0,previousJump-dt*crossing);
                float hop=jumpRemaining<=0?0:(float)Math.sin(Math.PI*(1-jumpRemaining/.85f));
                if(gap<0.48f) {
                    if(item.kind==Kind.COIN) { item.collected=true;coins++; if(events!=null)events.coin(); }
                    else if(item.kind==Kind.MUD) { finish("The tire got stuck in the mud."); return; }
                    else if(hop<0.35f || item.kind==Kind.CART) {
                        cleanHops=0;
                        strikes++; recovery=8; flash=0.45f;
                        if(strikes>=2) { finish("Two stumbles! The dog caught up."); return; }
                        if(events!=null)events.stumble();
                    } else {
                        cleanHops++;
                        if(hop>=.9f) { perfectHops++;award("PERFECT HOP",PERFECT_HOP_POINTS); }
                    }
                } else if(item.kind!=Kind.COIN && item.threatened && gap<1.08f
                    && previousTime+dt*crossing-lastSteerTime<=.4f) {
                    nearMisses++;award("CLOSE CALL",NEAR_MISS_POINTS);
                }
                checkMission();
            }
            if(item.z < -12) items.remove(i);
        }
    }
    private void award(String label,int points) {
        bonusPoints+=points;if(events!=null)events.skill(label,points);
    }
    private void checkMission() {
        if(missionComplete || missionProgress()<missionTarget())return;
        missionComplete=true;if(events!=null)events.missionComplete();
    }
    private void spawnRow() {
        rowNumber++;
        // Reserve the rows either side of each surprise. Ordinary hazards
        // cannot crowd the marked lane or its escape route at the crossing.
        int beat=rowNumber%6;
        if(beat==3 || beat==5)return;
        if(beat==4) {
            Kind[] surprises={Kind.CHICKEN,Kind.BASKET,Kind.CART};
            int target=random.nextInt(3)-1;
            items.add(new Item(surprises[surpriseNumber++%3],target,105));
            return;
        }
        int blocked=random.nextInt(3)-1;
        Kind kind=Kind.values()[1+random.nextInt(3)];
        items.add(new Item(kind,blocked,105));
        int safe=(blocked+1+random.nextInt(2)+1)%3-1;
        for(int i=0;i<3;i++) items.add(new Item(Kind.COIN,safe,105+i*4));
    }
    private void finish(String reason) { state=State.OVER; endReason=reason; if(events!=null)events.end(reason); }
}
