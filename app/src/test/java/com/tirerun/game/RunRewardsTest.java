package com.tirerun.game;

/** Rules for boosts, skill timing and mission rewards, independent of Android. */
public final class RunRewardsTest {
    private static void check(boolean value,String message) {
        if(!value)throw new AssertionError(message);
    }
    private static final class Events implements GameEngine.Events {
        int skills,missions,ends;
        public void coin() { }
        public void stumble() { }
        public void end(String reason) { ends++; }
        public void skill(String label,int points) { skills++; }
        public void missionComplete() { missions++; }
    }
    private static GameEngine fresh(Events events,int mission) {
        GameEngine game=new GameEngine(42,events);game.start(mission);game.items.clear();return game;
    }
    private static void cross(GameEngine game,GameEngine.Kind kind) {
        game.items.add(new GameEngine.Item(kind,0,.01f));game.update(.01f);
    }
    private static void perfectHop(GameEngine game) {
        game.jumpTime=.425f;cross(game,GameEngine.Kind.LOG);
    }
    public static void main(String[] args) {
        Events events=new Events();GameEngine g=fresh(events,0);
        float maxBoost=0;
        for(int metres=0;metres<8000;metres++) {
            g.distance=metres;
            float boost=g.downhillBoost();maxBoost=Math.max(maxBoost,boost);
            check(boost>=0 && boost<=1,"Boost stays bounded");
            check(g.speed()>=g.baseSpeed() && g.speed()<=38,"Boost has a playable speed cap");
            if(TrailCamera.slope(metres*TrailCamera.WORLD_SCALE)>=0)
                check(boost==0,"Uphill terrain never triggers a downhill boost");
            float speed=g.speed();g.distance+=.02f;
            check(Math.abs(g.speed()-speed)<.02f,"Boost ramps smoothly through hill transitions");
        }
        check(maxBoost>.99f,"The route contains full-strength downhill boosts");
        g=fresh(events,0);g.distance=150;g.pause();
        float distance=g.distance,boost=g.downhillBoost();g.update(.25f);
        check(g.distance==distance && g.downhillBoost()==boost,"Pause freezes boosted travel");

        events=new Events();g=fresh(events,0);
        GameEngine.Item hazard=new GameEngine.Item(GameEngine.Kind.ROCK,0,4);
        g.items.add(hazard);g.update(.01f);g.move(1);g.update(.25f);
        check(g.strikes==0 && g.nearMisses==1 && g.bonusPoints==25,"A late dodge earns one close-call bonus");
        check(events.skills==1,"Close call emits one notification");
        g.update(.25f);
        check(g.nearMisses==1 && events.skills==1,"Resolved hazards cannot reward twice");
        check(g.score()==(int)g.distance+25,"Score combines travel and skill points");

        g=fresh(new Events(),0);g.items.add(new GameEngine.Item(GameEngine.Kind.ROCK,1,.01f));g.update(.05f);
        check(g.nearMisses==0,"Passing a hazard in an adjacent lane is not a close call");
        g=fresh(new Events(),0);g.items.add(new GameEngine.Item(GameEngine.Kind.ROCK,0,18));g.move(1);
        for(int i=0;i<22;i++)g.update(.05f);
        check(g.nearMisses==0 && g.strikes==0,"An early safe lane change does not earn a timing bonus");
        g=fresh(new Events(),0);g.move(1);g.items.add(new GameEngine.Item(GameEngine.Kind.ROCK,0,.01f));g.update(.05f);
        check(g.strikes==1 && g.nearMisses==0,"A lane change after the crossing cannot dodge retrospectively");

        events=new Events();g=fresh(events,1);perfectHop(g);
        check(g.perfectHops==1 && g.bonusPoints==40 && g.cleanHops==1,"A hop at its apex earns skill points");
        g.update(.05f);check(events.skills==1,"A perfect hop rewards only once");
        g.jumpTime=.73f;cross(g,GameEngine.Kind.ROCK);
        check(g.cleanHops==2 && g.perfectHops==1,"A safe but imperfect hop progresses the mission without skill points");
        g.jumpTime=0;cross(g,GameEngine.Kind.ROCK);
        check(g.cleanHops==0 && !g.missionComplete,"A stumble breaks an unfinished clean-hop streak");
        for(int i=0;i<3;i++)perfectHop(g);
        check(g.missionComplete && events.missions==1,"Three clean hops finish the hop mission");
        g.jumpTime=0;cross(g,GameEngine.Kind.ROCK);
        check(g.state==GameEngine.State.OVER && g.missionComplete && events.missions==1,"Completed missions survive a later collision");
        g=fresh(new Events(),1);g.jumpTime=.425f;cross(g,GameEngine.Kind.MUD);
        check(g.state==GameEngine.State.OVER && g.perfectHops==0 && g.bonusPoints==0,"Mud cannot award a hop bonus");

        events=new Events();g=fresh(events,0);g.coins=11;cross(g,GameEngine.Kind.COIN);
        check(g.missionComplete && events.missions==1 && g.coins==12 && g.earnedCoins()==37,"Coin mission grants a separate 25-coin reward");
        for(int i=0;i<5;i++)g.update(.05f);
        check(events.missions==1 && g.earnedCoins()==37,"Mission rewards cannot be farmed on subsequent frames");
        g.start(1);
        check(!g.missionComplete && g.coins==0 && g.bonusPoints==0 && g.nearMisses==0 && g.missionTarget()==3,"Retry starts the next mission with fresh run counters");
        g=fresh(events,2);g.distance=349;g.update(.1f);
        check(g.missionComplete && g.missionProgress()==350,"Distance mission completes at its goal");
        g=fresh(events,0);g.coins=11;g.start(0);
        check(g.missionProgress()==0 && g.missionTarget()==12,"An unfinished mission retries from zero");
        check(GameEngine.missionTarget(300)==24 && GameEngine.missionTarget(301)==6 && GameEngine.missionTarget(302)==650,"Later mission goals stay bounded");
        g=fresh(new Events(),0);g.coins=11;g.pause();cross(g,GameEngine.Kind.COIN);
        check(!g.missionComplete && g.coins==11,"Paused gameplay cannot progress missions");
        System.out.println("PASS: downhill boost bounds, crossing-time dodges, perfect hops, scoring, mission progression and one-time rewards.");
    }
}
