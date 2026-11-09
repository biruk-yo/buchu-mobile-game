package com.tirerun.game;

/** Predictable warnings and safe escape routes, including at maximum downhill pace. */
public final class VillageSurprisesTest {
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    private static final class Events implements GameEngine.Events {
        int warnings;
        public void coin() { }
        public void stumble() { }
        public void end(String reason) { }
        public void warning(GameEngine.Kind kind) { warnings++; }
    }
    private static GameEngine game(Events events) {
        GameEngine g=new GameEngine(42,events);g.start();g.items.clear();
        for(int d=1500;d<4000;d++) {g.distance=d;if(g.speed()>37.9f)break;}
        return g;
    }
    private static void cross(GameEngine g,GameEngine.Item item,boolean hop) {
        while(!item.resolved && g.state==GameEngine.State.RUNNING) {
            g.items.removeIf(other -> other!=item);
            if(hop)g.jumpTime=.425f;
            g.update(.05f);
        }
    }
    public static void main(String[] args) {
        for(GameEngine.Kind kind:new GameEngine.Kind[]{GameEngine.Kind.CHICKEN,GameEngine.Kind.BASKET,GameEngine.Kind.CART}) {
            Events events=new Events();GameEngine g=game(events);
            GameEngine.Item item=new GameEngine.Item(kind,0,105);g.items.add(item);
            g.update(.05f);
            check(events.warnings==1 && g.approachingSurprise()==item,"Incoming surprise has a visible and audible warning");
            for(int i=0;i<5;i++)g.update(.05f);
            check(events.warnings==1,"Warning is emitted once while the same surprise approaches");
            float z=item.z,age=item.warningAge,elapsed=g.elapsed;
            g.pause();g.update(.25f);
            check(item.z==z && item.warningAge==age && g.elapsed==elapsed,"Pause freezes the warning and crossing");
            g.resume();cross(g,item,false);
            check(item.warningAge>=2.5f,"Even downhill, the player gets at least 2.5 seconds of warning");
            check(g.strikes==1 && g.approachingSurprise()==null,"A surprise resolves after its crossing");
            for(int i=0;i<5;i++)g.update(.05f);
            check(g.strikes==1,"Passing a surprise cannot hit again");
            g=game(new Events());item=new GameEngine.Item(kind,0,105);g.items.add(item);
            cross(g,item,true);
            check(kind==GameEngine.Kind.CART?g.strikes==1:g.strikes==0,"Only low obstacles can be hopped");
            g=game(new Events());item=new GameEngine.Item(kind,0,105);g.items.add(item);g.move(1);
            cross(g,item,false);check(g.strikes==0 && g.state==GameEngine.State.RUNNING,"Adjacent lane is a safe escape route");
            g=game(new Events());item=new GameEngine.Item(kind,0,.01f);g.items.add(item);g.update(.25f);
            check(g.strikes==0,"Late/unannounced surprise spawns cannot cause unavoidable hits");
            for(int lane=-1;lane<=1;lane++) {
                item=new GameEngine.Item(kind,lane,75);
                check(Math.abs(item.lateral())>2,"Surprises enter from outside the running lanes");
                item.z=18;check(item.lateral()==lane,"Obstacle settles in the announced lane before reaching the player");
                item.z=0;check(item.lateral()==lane,"Visual and collision lanes agree at contact");
            }
        }
        Events events=new Events();GameEngine g=new GameEngine(61,events);g.start();
        boolean chicken=false,basket=false,cart=false;
        for(int i=0;i<6000;i++) {
            g.items.removeIf(item -> item.z<5);g.update(.05f);
            int incoming=0;
            for(GameEngine.Item item:g.items)if(item.surprise()) {
                if(item.warned && !item.resolved)incoming++;
                chicken|=item.kind==GameEngine.Kind.CHICKEN;basket|=item.kind==GameEngine.Kind.BASKET;cart|=item.kind==GameEngine.Kind.CART;
                for(GameEngine.Item other:g.items)if(other!=item && other.kind!=GameEngine.Kind.COIN)
                    check(Math.abs(other.z-item.z)>=36,"Ordinary hazards leave a clear space around each surprise");
            }
            check(incoming<=1,"Only one surprise warning is active at a time");
        }
        check(chicken && basket && cart && events.warnings>3,"Long runs include all three village surprises");
        g.start();check(g.approachingSurprise()==null,"Retry clears stale warnings");
        System.out.println("PASS: village surprise rotation, warning lead time, escape lanes, hops, carts, pause and one-time collision.");
    }
}
