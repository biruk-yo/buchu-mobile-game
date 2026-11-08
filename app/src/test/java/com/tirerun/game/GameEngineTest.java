package com.tirerun.game;

/** Run with scripts/test-engine.sh; uses no Android runtime or downloaded test libraries. */
public final class GameEngineTest {
    private static int checks;
    private static void check(boolean truth,String message) { checks++;if(!truth)throw new AssertionError(message); }
    private static GameEngine fresh() { GameEngine g=new GameEngine(42,null);g.start();g.items.clear();return g; }
    private static void hit(GameEngine g,GameEngine.Kind kind) { g.items.add(new GameEngine.Item(kind,g.lane,.01f));g.update(.01f); }
    public static void main(String[] args) {
        GameEngine g=fresh();hit(g,GameEngine.Kind.COIN);check(g.coins==1,"Coin rewards once");g.update(.01f);check(g.coins==1,"Coin cannot be counted twice");
        check(g.items.get(0).collected,"Picked-up coins are marked to hide immediately");
        g=fresh();GameEngine.Item missed=new GameEngine.Item(GameEngine.Kind.COIN,1,.01f);g.items.add(missed);
        g.update(.05f);check(missed.resolved && !missed.collected && g.coins==0,"Missed coin is resolved but remains visible");
        for(int i=0;i<10;i++)g.update(.05f);
        check(g.items.contains(missed),"Missed coin remains as it travels to the lower screen edge");
        g.move(1);for(int i=0;i<20;i++)g.update(.05f);
        check(g.coins==0 && !missed.collected,"Changing lanes cannot collect a coin already behind the boy");
        check(!g.items.contains(missed),"Missed coin is removed after passing out of view");
        g=fresh();hit(g,GameEngine.Kind.ROCK);check(g.state==GameEngine.State.RUNNING && g.strikes==1,"First collision is survivable");hit(g,GameEngine.Kind.LOG);check(g.state==GameEngine.State.OVER,"Consecutive second collision ends run");
        g=fresh();hit(g,GameEngine.Kind.ROCK);for(int i=0;i<161;i++)g.update(.05f);check(g.strikes==0,"Eight safe seconds clear strike");hit(g,GameEngine.Kind.LOG);check(g.state==GameEngine.State.RUNNING,"Recovered runner survives another first strike");
        g=fresh();g.jump();for(int i=0;i<6;i++)g.update(.05f);hit(g,GameEngine.Kind.ROCK);check(g.strikes==0,"Hop clears rock");hit(g,GameEngine.Kind.LOG);check(g.strikes==0,"Hop clears log");hit(g,GameEngine.Kind.MUD);check(g.state==GameEngine.State.OVER,"Mud traps tire even during hop");
        g=fresh();g.move(-1);for(int i=0;i<15;i++)g.update(.02f);g.items.add(new GameEngine.Item(GameEngine.Kind.MUD,0,.01f));g.update(.01f);check(g.state==GameEngine.State.RUNNING,"Changing lane avoids mud");g.move(-1);check(g.lane==-1,"Cannot leave left lane");g.move(1);g.move(1);g.move(1);check(g.lane==1,"Cannot leave right lane");
        g=fresh();g.pause();float distance=g.distance;g.update(.05f);g.move(1);g.jump();check(g.distance==distance && g.lane==0 && g.jumpTime==0,"Pause freezes gameplay and input");g.resume();g.update(.05f);check(g.distance>distance,"Resume advances gameplay");
        g=fresh();g.distance=100000;check(g.speed()==23,"Speed has a playable upper bound");g.start();check(g.distance==0 && g.coins==0 && g.strikes==0 && g.lane==0,"Restart resets run");
        for(GameEngine.Kind kind:new GameEngine.Kind[]{GameEngine.Kind.ROCK,GameEngine.Kind.LOG,GameEngine.Kind.MUD}) {
            g=fresh();GameEngine.Item obstacle=new GameEngine.Item(kind,1,.01f);g.items.add(obstacle);
            g.update(.05f);
            check(obstacle.resolved && obstacle.z<0 && g.items.contains(obstacle),"Passed obstacle remains available for rendering");
            for(int i=0;i<12;i++)g.update(.05f);
            check(g.items.contains(obstacle),"Obstacle survives travel from boy to lower screen edge");
            g.move(1);for(int i=0;i<20;i++)g.update(.05f);
            check(g.state==GameEngine.State.RUNNING && g.strikes==0,"Passed obstacles cannot collide again");
            check(!g.items.contains(obstacle),"Obstacle is eventually removed behind the camera");
        }
        g=fresh();for(int i=0;i<5000;i++) { g.items.removeIf(item -> item.z<5);g.update(.05f);for(GameEngine.Item item:g.items)check(item.lane>=-1 && item.lane<=1,"Spawn remains on road"); }
        check(g.items.size()<45,"World objects remain bounded over long runs");
        System.out.println("PASS: "+checks+" rule checks (coins, collisions, recovery, mud, hops, lanes, pause, restart, long-run bounds).");
    }
}
