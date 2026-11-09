package com.tirerun.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.*;
import java.util.ArrayList;
import java.util.Comparator;

/** Native Canvas animation with a bundled generated mud texture. No runtime network requests. */
public final class GameView extends View implements GameEngine.Events {
    private static final int INK=Color.rgb(35,54,48), CREAM=Color.rgb(255,246,225),
        GOLD=Color.rgb(244,193,73), RUST=Color.rgb(190,74,43), GREEN=Color.rgb(55,101,76),
        SKIN=Color.rgb(111,65,43);
    private final Paint p=new Paint(3);
    private final android.graphics.drawable.Drawable brandIcon;
    private final Path path=new Path();
    private final RectF rect=new RectF();
    private final Typeface bold=Typeface.create("sans-serif-black",Typeface.BOLD);
    private final Typeface medium=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    private final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL);
    private final SharedPreferences prefs;
    final GameEngine game;
    private final GameAudio audio;
    private boolean sound, active=true, saved=false;
    private float W=432,H=960,scale=1, animation=0, downX,downY;
    private long lastFrame=0;
    private int best,bank,bestScore,missionsCompleted;
    private String skillToast="";
    private float toastTime=0,skillToastTime=0;
    private LinearGradient sky, forestFloor;
    private final DayNightCycle daylight=new DayNightCycle();
    private float runPhase,dogPhase;
    private int lastSkyTop,lastSkyBottom;
    private float skyHeight=-1;
    private final RunCycle runner=new RunCycle();
    private Bitmap mudTexture;
    private final Matrix mudMatrix=new Matrix();
    private final float[] mudSource=new float[8],mudTarget=new float[8];
    private float mudImpact;
    private final LinearGradient rubber=new LinearGradient(8,-110,65,-20,
        new int[]{0xff4b5049,0xff252a27,0xff101411,0xff363b35},null,Shader.TileMode.CLAMP);
    private final LinearGradient tireSide=new LinearGradient(8,0,72,0,
        new int[]{0xff111613,0xff41473f,0xff121713},null,Shader.TileMode.CLAMP);
    private final LinearGradient dogCoat=new LinearGradient(-22,0,23,0,
        new int[]{0xff202b38,0xff657685,0xff394957,0xff172330},null,Shader.TileMode.CLAMP);
    private final TrailCamera camera=new TrailCamera();
    private static final float[] TERRAIN_EDGES={-48,-24,-14,-7,0,7,14,24,48};
    private final VillageArt villageArt=new VillageArt();
    private boolean resetCamera=true;
    private boolean wasMenu;
    private float villageDistance;
    private final GameEngine.Item villageCoin=new GameEngine.Item(GameEngine.Kind.COIN,0,0);
    private final SceneNode[] scenePool=new SceneNode[512];
    private final ArrayList<SceneNode> scene=new ArrayList<>(512);
    private static final class SceneNode {
        float s,lateral,depth; int type,index; GameEngine.Item item;
    }

    public GameView(Context context) {
        super(context);
        brandIcon=context.getDrawable(R.mipmap.ic_launcher);
        for(int i=0;i<scenePool.length;i++)scenePool[i]=new SceneNode();
        setFocusableInTouchMode(true); requestFocus();
        prefs=context.getSharedPreferences("tire_run",Context.MODE_PRIVATE);
        best=prefs.getInt("best",0); bank=prefs.getInt("bank",0); sound=prefs.getBoolean("sound",true);
        bestScore=prefs.getInt("bestScore",best);missionsCompleted=prefs.getInt("missionsCompleted",0);
        audio=new GameAudio(context);
        game=new GameEngine(System.nanoTime(),this);
        try(java.io.InputStream stream=context.getAssets().open("mud-puddle.png")) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
            mudTexture=BitmapFactory.decodeStream(stream,null,options);
            float w=mudTexture.getWidth(),h=mudTexture.getHeight();
            mudSource[2]=w;mudSource[4]=w;mudSource[5]=h;mudSource[7]=h;
        } catch(java.io.IOException ignored) { }
        setContentDescription("ቡቹ (Buchu). Swipe left or right to steer; swipe up to hop. Avoid mud. Two hits within eight seconds end your run.");
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        scale=w/432f; H=h/scale;
        sky=new LinearGradient(0,0,0,H*.6f,new int[]{0xfff8eed7,0xfff1d2a4},null,Shader.TileMode.CLAMP);
        forestFloor=new LinearGradient(0,H*.43f,0,H,new int[]{0xff70836a,0xff4e6445,0xff2d4938},null,Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas) {
        audio.update(sound,active,game.state);
        long now=System.nanoTime();
        float dt=lastFrame==0?0:Math.min(GameEngine.MAX_FRAME_SECONDS,(now-lastFrame)/1e9f); lastFrame=now;
        if(active) {
            game.update(dt);
            if(game.state==GameEngine.State.RUNNING || game.state==GameEngine.State.MENU) {
                animation+=dt;
                float cadence=game.state==GameEngine.State.MENU?1.2f:RunCycle.cadence(game.speed());
                runPhase=RunCycle.cycle(runPhase+dt*cadence);
                dogPhase=RunCycle.cycle(dogPhase+dt*(cadence+.18f));
            }
        }
        daylight.sample(animation);
        if(active && game.state==GameEngine.State.RUNNING) {
            toastTime=Math.max(0,toastTime-dt);skillToastTime=Math.max(0,skillToastTime-dt);
        }
        if(active)mudImpact=Math.max(0,mudImpact-dt);
        canvas.save(); canvas.scale(scale,scale);
        boolean menu=game.state==GameEngine.State.MENU;
        if(menu) {
            if(!wasMenu) {
                villageDistance=game.distance>0?game.distance:155;
                resetCamera=true;
            }
            // This is a presentation-only camera tour; it does not advance the run.
            if(active)villageDistance+=dt*4.7f;
            camera.update(villageDistance,0,H,active?dt:0,resetCamera);
            // Leave room for the title and start card without changing the world art.
            camera.height=(H-270-camera.horizon)*camera.back/camera.focal;
        } else {
            camera.update(game.distance,game.visualLane,H,
                game.state==GameEngine.State.RUNNING?dt:0,resetCamera);
        }
        resetCamera=false;wasMenu=menu;
        drawChaseWorld(canvas);
        applyWorldLight(canvas);
        if(menu) drawMenu(canvas); else {
            drawHud(canvas);
            if(game.state==GameEngine.State.PAUSED) drawPause(canvas);
            if(game.state==GameEngine.State.OVER && mudImpact<=0) drawGameOver(canvas);
        }
        canvas.restore();
        if(active && (game.state==GameEngine.State.RUNNING || game.state==GameEngine.State.MENU || mudImpact>0)) postInvalidateOnAnimation();
    }
    private void drawSky(Canvas c,float skyline) {
        if(sky==null || lastSkyTop!=daylight.skyTop || lastSkyBottom!=daylight.skyBottom || skyHeight!=skyline) {
            sky=new LinearGradient(0,0,0,skyline,daylight.skyTop,daylight.skyBottom,Shader.TileMode.CLAMP);
            lastSkyTop=daylight.skyTop;lastSkyBottom=daylight.skyBottom;skyHeight=skyline;
        }
        p.setColor(Color.WHITE);p.setShader(sky);c.drawRect(0,0,W,H,p);p.setShader(null);
        if(daylight.night>.05f) {
            for(int i=0;i<52;i++) {
                float x=12+((i*97+31)%409),y=24+((i*67+19)%293)/293f*(skyline-70);
                float twinkle=.83f+.17f*(float)Math.sin(animation*.7f+i*2.4f);
                int alpha=(int)(daylight.night*235*twinkle);
                circle(c,x,y,i%7==0?1.25f:.7f,(alpha<<24)|0xeaf1df);
            }
        }
        float celestialRise=game.state==GameEngine.State.MENU?skyline*.16f:skyline-170;
        if(daylight.sunHeight>-.12f) {
            float x=daylight.sunX*W,y=skyline+10-daylight.sunHeight*celestialRise;
            circle(c,x,y,43,0x20ffd582);circle(c,x,y,32,0x35ffd582);
            circle(c,x,y,24,DayNightCycle.mix(0xfff7d377,0xffee9b56,daylight.twilight));
            circle(c,x-4,y-5,16,0x30fff2b0);
        }
        if(daylight.moonHeight>-.12f) {
            float x=daylight.moonX*W,y=skyline+10-daylight.moonHeight*celestialRise;
            circle(c,x,y,29,0x1fd8e8ef);circle(c,x,y,17,0xffe2e9de);
            circle(c,x-5,y+3,4,0xffb6c8cc);circle(c,x+6,y-6,3,0xffcad8d4);
            circle(c,x+5,y+8,2,0xffc1d0cf);
        }
    }
    private void applyWorldLight(Canvas c) {
        if(daylight.twilight>.01f)box(c,0,0,W,H,((int)(daylight.twilight*23)<<24)|0xde7e4d);
        if(daylight.night>.001f)box(c,0,0,W,H,daylight.worldTint);
    }
    private int outsideText() { return daylight.night>.18f?CREAM:INK; }
    private void drawChaseWorld(Canvas c) {
        boolean menu=game.state==GameEngine.State.MENU;
        float travelled=(menu?villageDistance:game.distance)*TrailCamera.WORLD_SCALE;
        float h=camera.skyline();
        drawSky(c,h);
        poly(c,0xffaab89b,0,h+5,0,h-53,58,h-84,119,h-43,172,h-101,220,h-76,285,h-99,346,h-49,432,h-74,432,h+20);
        poly(c,0xff7f9677,0,h+12,0,h-19,74,h-46,147,h-24,210,h-53,284,h-22,355,h-44,432,h-15,432,h+30);
        p.setShader(forestFloor);c.drawRect(0,h,W,H,p);p.setShader(null);
        // Distant tree line closes the horizon; near trees are world-anchored below.
        for(int i=0;i<23;i++) {
            float tx=i*22-25-(float)Math.sin(camera.yaw)*30;
            oval(c,tx-16,h-53-(i%4)*10,tx+18,h+10, i%2==0?0xff64836a:0xff729074);
        }
        scene.clear();
        // Reserve gameplay entries before adding decorative scenery.
        if(!menu)for(GameEngine.Item item:game.items) {
            // Resolving a collision does not remove the physical obstacle. Keep it
            // in the depth-sorted world until it passes out of the camera view.
            if(item.collected)continue;
            addScene(camera.position+item.z*TrailCamera.WORLD_SCALE,item.lateral()*TrailCamera.LANE_WIDTH,4,0,item);
        }
        if(menu) {
            int firstCoin=(int)Math.floor(camera.position/4)+1;
            for(int i=firstCoin;i<firstCoin+5;i++)addScene(i*4,0,4,0,villageCoin);
        }
        float runnerLane=menu?0:game.visualLane*TrailCamera.LANE_WIDTH;
        addScene(camera.position,runnerLane,5,0,null);
        addScene(camera.position-(!menu && game.strikes>0?.12f:.42f),runnerLane+.72f,6,0,null);
        int base=(int)Math.floor(camera.position/3.2f);
        for(int i=base-1;i<=base+16;i++) {
            float s=i*3.2f;
            boolean compound=Math.floorMod((int)(s/(180*TrailCamera.WORLD_SCALE)),3)==1;
            for(int side=-1;side<=1;side+=2) {
                int id=Math.floorMod(i*17+side*13,19);
                // Trees frame the yards, with lower scenery close to the shoulder.
                if(id%3!=0)addScene(s,side*(5.7f+(id%3)*.7f),0,id,null);
                if(id%2==0)addScene(s+1.1f,side*(7.9f+(id%4)),0,id+4,null);
                addScene(s+.7f,side*2.55f,3,id,null);
                addScene(s+1.8f,side*2.75f,VillageArt.HEDGE,id,null);
                int plot=Math.floorMod(i+(side==1?2:0),compound?4:6);
                if(plot==0 || plot==2) {
                    addScene(s+1.4f,side*4.8f,1,id,null);
                    addScene(s-.4f,side*3.0f,2,id,null);
                    addScene(s+.2f,side*3.65f,VillageArt.HOUSEHOLD,id,null);
                    addScene(s-.1f,side*3.15f,VillageArt.CHICKENS,id,null);
                    if(plot==2)addScene(s+2.5f,side*4.5f,VillageArt.LAUNDRY,id,null);
                } else if(plot==1) {
                    addScene(s+1.1f,side*3.8f,VillageArt.STALL,id,null);
                    addScene(s+2.5f,side*2.9f,VillageArt.NEIGHBOR,id,null);
                } else {
                    addScene(s+1.4f,side*3.5f,VillageArt.GARDEN,id,null);
                    addScene(s+.3f,side*4.7f,8,id,null);
                    if(plot==3)addScene(s+2.5f,side*3.2f,VillageArt.HOUSEHOLD,id,null);
                }
            }
        }
        int verge=(int)Math.floor(camera.position/1.4f);
        for(int i=verge;i<verge+22;i++)for(int side=-1;side<=1;side+=2)
            addScene(i*1.4f,side*2.35f,VillageArt.STONES,Math.floorMod(i,13),null);
        // Compound entrances span the route. Their opening always clears all three lanes.
        int gate=(int)Math.floor(camera.position/24);
        for(int i=gate;i<=gate+2;i++)addScene(i*24+12,0,7,i,null);
        // Paint terrain and objects together from far to near: a hill crest
        // naturally hides the lower part of scenery on the far side.
        scene.sort(Comparator.comparingDouble(node -> -node.s));
        int nextNode=0;
        float far=camera.position+96;
        for(float b=far;b>camera.position-3;b-=b-camera.position>24?1.25f:.5f) {
            float a=b-(b-camera.position>24?1.25f:.5f);
            if(camera.depth(a,0)<.65f)continue;
            drawTerrain(c,a,b);
            // Clip each part of a puddle to its own terrain slice so nearer
            // slices do not erase the front half of this ground-level decal.
            for(SceneNode node:scene) {
                if(node.type!=4 || node.item.kind!=GameEngine.Kind.MUD || node.s+.98f<a || node.s-.98f>b)continue;
                c.save();trailQuad(a,b,-2.16f,2.16f);c.clipPath(path);
                mudOnTrail(c,node);c.restore();
            }
            // The illustrations include feet and shadows in front of their
            // anchor. Paint after that footprint, not through their ankles.
            while(nextNode<scene.size() && scene.get(nextNode).s>=a+1.2f) {
                drawSceneNode(c,scene.get(nextNode++),menu,travelled);
            }
        }
        while(nextNode<scene.size())drawSceneNode(c,scene.get(nextNode++),menu,travelled);
        if(!menu && game.state==GameEngine.State.RUNNING)drawBoostWind(c);
        if(!menu && game.flash>0)box(c,0,0,W,H,((int)(game.flash*100)<<24)|0xCA472A);
    }
    private void drawSceneNode(Canvas c,SceneNode node,boolean menu,float travelled) {
        float x=camera.screenX(node.s,node.lateral),y=camera.screenY(node.s,node.lateral,0);
        float q=camera.scale(node.s,node.lateral)/90;
        // Ground decals can still cross the bottom edge after their centre
        // leaves the screen; let Canvas clip items to their full visible bounds.
        if(x < -220*q || x>W+220*q || (node.type!=4 && y>H+270))return;
        switch(node.type) {
            case 0: forestTree(c,x,y,q,node.index);break;
            case 1:
                hut(c,x,y,q*1.7f,node.index);
                // Woven cloth and a clay water pot outside the compound.
                round(c,x-38*q,y-39*q,x-24*q,y-10*q,2*q,0xffe4d6ad);
                line(c,x-37*q,y-28*q,x-25*q,y-28*q,RUST,3*q);
                oval(c,x+35*q,y-22*q,x+51*q,y+1*q,0xffa65f3e);
                break;
            case 2: compoundFence(c,node.s,node.lateral);break;
            case 3: fern(c,x,y,q*.75f);break;
            case 7: compoundGate(c,node.s);break;
            case 8: banana(c,x,y,q);break;
            case 4:
                switch(node.item.kind) {
                    case COIN:drawCoin(c,x,y-23*q,q,animation*3+node.item.z);break;
                    case MUD:break; // Already painted onto the ground.
                    case ROCK:rock(c,x,y,q);break;
                    case LOG:log(c,x,y,q);break;
                    case CHICKEN:case BASKET:case CART:
                        villageArt.surprise(c,node.item.kind,x,y,q,animation,105-node.item.z,node.item.lane==1?-1:1);break;
                }
                break;
            case 5:
                if(!menu && game.downhillBoost()>.05f) {
                    float boost=game.downhillBoost();
                    for(int i=0;i<6;i++) {
                        float phase=RunCycle.cycle(animation*1.7f+i/6f);
                        float dx=(i%2==0?-1:1)*(20+phase*37)*q;
                        circle(c,x+dx,y+(8+phase*17)*q,(2+phase*6)*q,
                            ((int)((1-phase)*65*boost)<<24)|0xe7ce9b);
                    }
                }
                oval(c,x-29*q,y-5*q,x+39*q,y+8*q,0x40302d20);
                c.save();c.translate(x,y-(menu?0:game.jumpHeight()*50*q)+(mudImpact>0 && !menu?(1-mudImpact/.75f)*8*q:0));c.scale(q,q);
                if(!menu && game.flash>0 && ((int)(game.flash*18))%2==0)c.saveLayerAlpha(-70,-205,90,30,155);
                else c.save();
                chaseBoy(c,travelled);
                if(!menu && mudImpact>0)mudSplash(c,runner.tireX,runner.tireY+46*RunCycle.TIRE_SCALE,1-mudImpact/.75f);
                c.restore();c.restore();break;
            case 6:dog(c,x,y,q*.78f,travelled);break;
            default:villageArt.draw(c,node.type,x,y,q,node.index,animation);break;
        }
    }
    private void drawBoostWind(Canvas c) {
        float boost=game.downhillBoost();if(boost<.05f)return;
        for(int i=0;i<12;i++) {
            float phase=RunCycle.cycle(animation*(1.1f+boost)+i/12f);
            float x=i%2==0?8+(i%3)*9:W-8-(i%3)*9;
            float y=H*(.32f+phase*.58f);
            int alpha=(int)(boost*95*(float)Math.sin(phase*Math.PI));
            line(c,x,y,x+(i%2==0?-5:5),y+18+boost*24,(alpha<<24)|0xffefd0,1.2f);
        }
    }
    private void addScene(float s,float lateral,int type,int index,GameEngine.Item item) {
        float depth=camera.depth(s,lateral);
        if(depth<.75f || scene.size()>=scenePool.length)return;
        SceneNode node=scenePool[scene.size()];node.s=s;node.lateral=lateral;
        node.type=type;node.index=index;node.depth=depth;node.item=item;scene.add(node);
    }
    private void drawTerrain(Canvas c,float a,float b) {
        float light=Math.max(0,Math.min(1,.5f+TrailCamera.slope((a+b)*.5f)*1.3f));
        // Broad strips give the road a continuous hillside instead of a flat backdrop.
        for(int i=0;i<TERRAIN_EDGES.length-1;i++) {
            int grass=DayNightCycle.mix(i<4?0xff536f44:0xff617c49,
                i<4?0xff819657:0xff91a263,light);
            roadStrip(c,a,b,TERRAIN_EDGES[i],TERRAIN_EDGES[i+1],grass);
        }
        for(int side=-1;side<=1;side+=2) {
            int yard=Math.floorMod((int)Math.floor((a+side*2)/6.4f),4);
            if(yard<2)roadStrip(c,a,b,side*2.55f,side*(yard==0?6.8f:5.2f),yard==0?0xff8c8960:0xff788455);
        }
        roadStrip(c,a,b,-2.7f,2.7f,0xff73805a);
        roadStrip(c,a,b,-2.48f,-2.22f,0xff9d9566);
        roadStrip(c,a,b,2.22f,2.48f,0xff9d9566);
        roadStrip(c,a,b,-2.16f,2.16f,0xffb49c65);
        roadStrip(c,a,b,-1.98f,1.98f,DayNightCycle.mix(0xffa97d50,0xffc49a65,light));
        roadStrip(c,a,b,-1.67f,-1.38f,DayNightCycle.mix(0xffb18554,0xffcda16c,light));
        roadStrip(c,a,b,1.38f,1.67f,DayNightCycle.mix(0xffb18554,0xffcda16c,light));
        GameEngine.Item warning=game.state==GameEngine.State.RUNNING?game.approachingSurprise():null;
        if(warning!=null && a>camera.position+1 && b<camera.position+Math.min(12,warning.z*TrailCamera.WORLD_SCALE)) {
            float l=warning.lane*TrailCamera.LANE_WIDTH;
            roadStrip(c,a,b,l-.52f,l+.52f,0x45ffcf4f);
            if(Math.floorMod((int)(a*2),4)==0)roadStrip(c,a,b,l-.52f,l+.52f,0x60fff0b2);
        }
        int tile=(int)Math.floor(a*1.34f);
        if(Math.floorMod(tile,11)<2)roadStrip(c,a,b,-1.96f,.7f,0x0c405536);
        int pebble=(int)Math.floor(b/1.7f);
        float s=pebble*1.7f;
        if(s>=a && s<b) {
            float side=(float)Math.sin(pebble*38.7)*1.8f;
            float q=camera.scale(s,side)/100,x=camera.screenX(s,side),y=camera.screenY(s,side,0);
            oval(c,x-3*q,y-q,x+3*q,y+q,0xff9e794e);
        }
    }
    private void trailQuad(float a,float b,float l,float r) {
        path.reset();path.moveTo(camera.screenX(a,l),camera.screenY(a,l,0));
        path.lineTo(camera.screenX(b,l),camera.screenY(b,l,0));
        path.lineTo(camera.screenX(b,r),camera.screenY(b,r,0));
        path.lineTo(camera.screenX(a,r),camera.screenY(a,r,0));path.close();
    }
    private void roadStrip(Canvas c,float a,float b,float l,float r,int color) {
        trailQuad(a,b,l,r);p.setColor(color);
        // Adjacent filled mesh faces share exact edges; antialiasing each face
        // separately leaves translucent seams across the grassy slopes.
        p.setAntiAlias(false);c.drawPath(path,p);p.setAntiAlias(true);
    }
    private void compoundFence(Canvas c,float s,float lateral) {
        for(int i=0;i<7;i++) {
            float a=s+i*.36f;
            line(c,camera.screenX(a,lateral),camera.screenY(a,lateral,0),
                camera.screenX(a,lateral),camera.screenY(a,lateral,.85f+(i%2)*.08f),0xff79603d,camera.scale(a,lateral)*.055f);
        }
        for(float h=.18f;h<.8f;h+=.14f)
            line(c,camera.screenX(s,lateral),camera.screenY(s,lateral,h),
                camera.screenX(s+2.2f,lateral),camera.screenY(s+2.2f,lateral,h),0xffa58a59,camera.scale(s,lateral)*.045f);
    }
    private void compoundGate(Canvas c,float s) {
        float left=camera.screenX(s,-2.35f),right=camera.screenX(s,2.35f);
        float ly=camera.screenY(s,-2.35f,0),ry=camera.screenY(s,2.35f,0);
        float lt=camera.screenY(s,-2.35f,3.2f),rt=camera.screenY(s,2.35f,3.2f);
        float width=camera.scale(s,0)*.11f;
        line(c,left,ly,left,lt,0xff685438,width);line(c,right,ry,right,rt,0xff685438,width);
        line(c,left-4,lt,right+4,rt,0xff7c6945,width);
        // Small cloth strips hang above head height, leaving the trail unobstructed.
        for(int i=1;i<8;i++) {
            float f=i/8f,x=left+(right-left)*f,y=lt+(rt-lt)*f;
            poly(c,i%3==0?RUST:i%3==1?GREEN:GOLD,x-1.6f*width,y,x+1.6f*width,y,x,y+4*width);
        }
    }
    private void forestTree(Canvas c,float x,float y,float q,int seed) {
        c.save();c.translate(x,y);c.scale(q,q);
        float height=450+(seed%4)*42;
        oval(c,-45,-6,48,10,0x2532482b);
        poly(c,0xff666449,-13,0,-7,-height,7,-height,16,0);
        line(c,-1,0,-1,-height,0xff9b9a73,3);
        line(c,0,-height*.65f,-46,-height*.87f,0xff6d6d4e,6);
        line(c,5,-height*.8f,49,-height*.98f,0xff6d6d4e,5);
        for(int i=0;i<5;i++) {
            float cx=(i-2)*31,cy=-height-14+(i%2)*20;
            oval(c,cx-45,cy-46,cx+42,cy+40,i%2==0?0xff335d43:0xff48744e);
            oval(c,cx-25,cy-39,cx+34,cy+4,0xff5e8557);
        }
        fern(c,-12,0,.7f);c.restore();
    }
    private void fern(Canvas c,float x,float y,float q) {
        c.save();c.translate(x,y);c.scale(q,q);
        for(int i=-3;i<=3;i++) {
            float xx=i*12,yy=-25+Math.abs(i)*4;
            line(c,0,0,xx,yy,0xff6d9756,2);
            poly(c,i%2==0?0xff4b7d45:0xff7d9d59,0,-1,xx-8,yy-8,xx,yy,xx+5,yy+7);
        }
        c.restore();
    }
    private void banana(Canvas c,float x,float y,float q) {
        c.save();c.translate(x,y);c.scale(q,q);
        line(c,0,0,0,-91,0xff7c8a50,7);
        for(int i=-2;i<=2;i++) {
            poly(c,i%2==0?0xff477c49:0xff67934f,0,-88,i*24-17,-118+Math.abs(i)*14,
                i*29,-103+Math.abs(i)*16,i*29+12,-58+Math.abs(i)*5,i*21,-88+Math.abs(i)*12);
        }
        c.restore();
    }
    private void chaseBoy(Canvas c,float metres) {
        runner.sample(runPhase,metres,game.state==GameEngine.State.MENU?0:game.jumpHeight());
        float bob=runner.bob,sway=runner.sway;
        // The tire is farther along the trail than the boy, so draw it first.
        c.save();c.translate(runner.tireX,runner.tireY);c.scale(RunCycle.TIRE_SCALE,RunCycle.TIRE_SCALE);
        rollingTire(c,0,0,runner.tireAngle);c.restore();
        boyLeg(c,runner.left,true);boyLeg(c,runner.right,false);
        arm(c,runner.leftArm);arm(c,runner.rightArm);
        grip(c,runner.leftArm.handX,runner.leftArm.handY,-1);
        grip(c,runner.rightArm.handX,runner.rightArm.handY,1);
        c.save();c.translate(sway,bob);
        round(c,-40,-81,10,-51,4,0xff315b4b);
        poly(c,0xff264b3d,-16,-76,9,-78,10,-51,-12,-51);
        line(c,-15,-68,-15,-51,0xff193f31,2);
        // A little forward lean and shoulder motion, while preserving the woven shirt.
        path.reset();path.moveTo(-36,-139);path.cubicTo(-26,-143,-7,-142,3,-137);
        path.cubicTo(9,-119,7,-99,12,-77);path.lineTo(-40,-77);
        path.cubicTo(-38,-104,-41,-121,-36,-139);path.close();
        p.setColor(0xfff1e5c8);c.drawPath(path,p);
        poly(c,0xfffff3d5,-33,-136,-18,-141,-16,-80,-37,-80);
        poly(c,0xffd6c7a5,2,-136,6,-120,7,-96,12,-77,2,-77,-2,-112);
        line(c,-24,-130,-26,-108,0xffded1b1,1.2f);
        line(c,-6,-106,-2,-94,0xffcdbf9d,1.1f);
        box(c,-39,-90,11,-85,GREEN);box(c,-39,-85,11,-81,GOLD);
        box(c,-39,-81,11,-77,RUST);
        for(int i=-34;i<=6;i+=10)poly(c,CREAM,i,-89,i+3,-84,i,-80,i-3,-84);
        round(c,-22,-151,-8,-135,3,SKIN);
        oval(c,-34,-178,3,-140,0xff895133);
        oval(c,-29,-176,2,-145,0xff9b603e);
        circle(c,-33,-158,3.5f,0xff8e5739);circle(c,3,-158,3.5f,0xff8e5739);
        oval(c,-34,-182,4,-151,0xff252a22);
        for(int i=0;i<8;i++)circle(c,-31+i*4.5f,-177-(i%2)*2,4.5f,0xff252a22);
        for(int i=0;i<10;i++)circle(c,-29+(i%5)*6,-172+(i/5)*7,1.2f,0xff3a3a2d);
        line(c,-30,-151,-28,-146,0xff3a3125,2);
        c.restore();
    }
    private void boyLeg(Canvas c,RunCycle.Leg leg,boolean far) {
        int skin=far?0xff75452f:0xff8d5336;
        line(c,leg.hipX,leg.hipY,leg.kneeX,leg.kneeY,skin,12);
        line(c,leg.kneeX,leg.kneeY,leg.footX,leg.footY,skin,9);
        line(c,leg.kneeX-2,leg.kneeY+1,leg.footX-2,leg.footY-4,far?0xff925b3b:0xffad7048,2.5f);
        circle(c,leg.kneeX,leg.kneeY,5.4f,skin);
        c.save();c.translate(leg.footX,leg.footY);
        c.rotate(far?-2:2);
        float sole=6+leg.lift*.18f;
        round(c,-7,-3,7,sole,3,0xff293b30);
        round(c,-5,-1,5,sole-1,2,0xffb59c6a);
        line(c,-6,0,6,0,0xff5f5c40,3);
        if(leg.lift>12) {
            line(c,-3,3,3,3,0xff706a47,1);
            line(c,-3,6,3,6,0xff706a47,1);
        }
        c.restore();
    }
    private void arm(Canvas c,RunCycle.Arm arm) {
        line(c,arm.shoulderX,arm.shoulderY,arm.elbowX,arm.elbowY,0xff7b482f,11);
        line(c,arm.shoulderX,arm.shoulderY,
            arm.shoulderX+(arm.elbowX-arm.shoulderX)*.47f,
            arm.shoulderY+(arm.elbowY-arm.shoulderY)*.47f,CREAM,14);
        line(c,arm.elbowX,arm.elbowY,arm.handX,arm.handY,0xff895135,9);
        line(c,arm.elbowX-1.5f,arm.elbowY-1,arm.handX-1.5f,arm.handY-1,0xffa86c47,2);
    }
    private void grip(Canvas c,float x,float y,int side) {
        c.save();c.translate(x,y);c.rotate(side*24);
        round(c,-5,-5,5,4,3,0xffa96f49);
        for(int i=0;i<4;i++) {
            line(c,-4+i*2.4f,-1,-4+i*2.4f,6.5f,0xff7d492f,2.4f);
            line(c,-4+i*2.4f,-1,-4+i*2.4f,3.8f,0xffba8055,1.3f);
        }
        line(c,-side*4,-2,-side*7,2,0xffa96f49,3.2f);
        c.restore();
    }
    private void rollingTire(Canvas c,float cx,float cy,float angle) {
        c.save();c.translate(cx-35,cy+66);
        // A tight contact shadow keeps the rolling rubber visibly on the path.
        oval(c,8,-24,72,-15,0x29362a1e);
        oval(c,22,-23,59,-18,0x48362a1e);
        // Back shoulder and inner cavity create depth while keeping the hole transparent.
        tireRing(c,43,-66,27,46,14,32,tireSide);
        c.save();path.reset();path.addOval(16,-112,70,-20,Path.Direction.CW);c.clipPath(path);
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/12+angle;
            if(Math.cos(a)<-.1)continue;
            float x=35+27*(float)Math.cos(a),y=-66+46*(float)Math.sin(a);
            float tangent=(float)Math.cos(a)*3;
            line(c,x+1,y,x+9,y+tangent,0xff080d0b,2.8f);
            line(c,x+2,y-1,x+8,y+tangent-1,0xff5c6257,.8f);
            if(i%3==0)line(c,x+5,y+1,x+8,y+4,0xff1e251f,1.2f);
        }
        c.restore();
        tireRing(c,35,-66,27,46,15,33,rubber);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(0xff686f60);
        c.drawOval(10,-109,60,-23,p);
        p.setColor(0xff0d130f);p.setStrokeWidth(2.4f);c.drawOval(19,-101,51,-31,p);
        p.setColor(0xff5a6154);p.setStrokeWidth(.8f);c.drawOval(17,-103,53,-29,p);
        p.setStyle(Paint.Style.FILL);
        // Uneven wear breaks the repeating pattern that made the spinning wheel look still.
        for(int i=0;i<24;i++) {
            double a=angle+i*2.399963;
            float x=35+23*(float)Math.cos(a),y=-66+40*(float)Math.sin(a);
            float xx=35+25*(float)Math.cos(a),yy=-66+43*(float)Math.sin(a);
            line(c,x,y,xx,yy,i%5==0?0xff93917a:0xff4b5548,i%5==0?1.3f:.7f);
        }
        // A worn chalk stripe and one dirt patch visibly orbit with the rubber.
        // Rotate in the circular wheel plane, then foreshorten it into the sidewall ellipse.
        c.save();c.translate(35,-66);c.scale(27f/46f,1);
        c.rotate((float)Math.toDegrees(angle));
        line(c,35,0,43,0,0xffd8cfad,3.4f);
        line(c,36,3.2f,42,3.2f,0xffa6a38c,1.4f);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.5f);p.setColor(0xffa58b60);
        c.drawArc(-39,-39,39,39,127,15,false,p);
        p.setStrokeWidth(1.3f);p.setColor(0xffc7ad7c);
        c.drawArc(-41,-41,41,41,131,8,false,p);
        p.setStyle(Paint.Style.FILL);c.restore();
        c.restore();
    }
    private void tireRing(Canvas c,float x,float y,float rx,float ry,float ix,float iy,Shader shade) {
        path.reset();path.setFillType(Path.FillType.EVEN_ODD);
        path.addOval(x-rx,y-ry,x+rx,y+ry,Path.Direction.CW);
        path.addOval(x-ix,y-iy,x+ix,y+iy,Path.Direction.CW);
        p.setColor(Color.WHITE);p.setShader(shade);c.drawPath(path,p);p.setShader(null);
        path.setFillType(Path.FillType.WINDING);
    }

    private void hut(Canvas c,float x,float y,float s,int variation) {
        villageArt.house(c,x,y,s,variation,animation,daylight.night);
    }
    private void drawCoin(Canvas c,float x,float y,float s,float t) {
        float width=(.75f+.25f*Math.abs((float)Math.cos(t)))*12*s;
        oval(c,x-width-2*s,y-15*s,x+width+2*s,y+15*s,0xffae7529);
        oval(c,x-width,y-16*s,x+width,y+12*s,GOLD);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f*s); p.setColor(0xffffe59c);
        c.drawOval(x-width+3*s,y-12*s,x+width-3*s,y+8*s,p); p.setStyle(Paint.Style.FILL);
        line(c,x,y-7*s,x,y+3*s,0xffb6802c,2*s);
        circle(c,x-4*s,y-9*s,2*s,CREAM);
    }
    private void mudOnTrail(Canvas c,SceneNode node) {
        float s=node.s,l=node.lateral,halfWidth=.73f,halfLength=.98f;
        // Map the transparent overhead decal onto the actual curved ground plane.
        mudTarget[0]=camera.screenX(s+halfLength,l-halfWidth);
        mudTarget[1]=camera.screenY(s+halfLength,l-halfWidth,0);
        mudTarget[2]=camera.screenX(s+halfLength,l+halfWidth);
        mudTarget[3]=camera.screenY(s+halfLength,l+halfWidth,0);
        mudTarget[4]=camera.screenX(s-halfLength,l+halfWidth);
        mudTarget[5]=camera.screenY(s-halfLength,l+halfWidth,0);
        mudTarget[6]=camera.screenX(s-halfLength,l-halfWidth);
        mudTarget[7]=camera.screenY(s-halfLength,l-halfWidth,0);
        if(mudTexture!=null) {
            mudMatrix.setPolyToPoly(mudSource,0,mudTarget,0,4);
            p.setColor(Color.WHITE);p.setFilterBitmap(true);c.drawBitmap(mudTexture,mudMatrix,p);
        } else {
            poly(c,0xff6c4931,mudTarget);
        }
        // Quiet concentric ripples break the reflected sky, without a cartoon label.
        float x=camera.screenX(s,l),y=camera.screenY(s,l,0),q=camera.scale(s,l)/90;
        float phase=RunCycle.cycle(animation*.45f+s*.19f);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.65f*q);
        p.setColor(((int)((1-phase)*52)<<24)|0xead5aa);
        c.drawOval(x-(4+phase*13)*q,y-(1+phase*3)*q,x+(4+phase*13)*q,y+(1+phase*3)*q,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void mudSplash(Canvas c,float x,float y,float progress) {
        for(int i=0;i<14;i++) {
            float direction=(i-6.5f)/6.5f;
            float xx=x+direction*(9+progress*48);
            float yy=y-(18+(i%4)*7)*4*progress*(1-progress)+progress*8;
            oval(c,xx-2.3f,yy-3.1f,xx+2.3f,yy+2.2f,i%2==0?0xff6c4129:0xff98704a);
        }
        oval(c,x-21,y-2,x+25,y+7,0x955b3d29);
    }
    private void rock(Canvas c,float x,float y,float s) {
        oval(c,x-31*s,y-5*s,x+33*s,y+10*s,0x383e3b2b);
        poly(c,0xff747a68,x-29*s,y,x-24*s,y-25*s,x-8*s,y-39*s,x+13*s,y-34*s,x+29*s,y-9*s,x+20*s,y+4*s);
        poly(c,0xffa4a58a,x-24*s,y-25*s,x-8*s,y-39*s,x+13*s,y-34*s,x+5*s,y-16*s);
        poly(c,0xff858c75,x+5*s,y-16*s,x+13*s,y-34*s,x+29*s,y-9*s,x+20*s,y+4*s);
    }
    private void log(Canvas c,float x,float y,float s) {
        round(c,x-38*s,y-25*s,x+38*s,y+3*s,6*s,0xff765137);
        line(c,x-30*s,y-17*s,x+29*s,y-17*s,0xffa77948,3*s);
        line(c,x-33*s,y-7*s,x+28*s,y-7*s,0xff553f2d,2*s);
        oval(c,x+23*s,y-25*s,x+44*s,y+3*s,0xffd2a36b);
        oval(c,x+29*s,y-19*s,x+39*s,y-3*s,0xff9c744c);
        oval(c,x+32*s,y-15*s,x+37*s,y-6*s,0xffcfa16b);
    }
    private void dog(Canvas c,float x,float y,float scale,float metres) {
        c.save();c.translate(x,y);c.scale(scale,scale);
        float phase=dogPhase,a=phase*RunCycle.TAU;
        float bounce=-1.5f-1.5f*(float)Math.cos(a*2);
        oval(c,-25,2,27,12,0x30342920);
        // Low, foreshortened body with diagonal foot pairs: a rear view on all four paws.
        dogLeg(c,-12,-43+bounce,-19,-12,phase,true);
        dogLeg(c,12,-43+bounce,19,-12,RunCycle.cycle(phase+.5f),true);
        dogLeg(c,-16,-22+bounce,-21,7,RunCycle.cycle(phase+.5f),false);
        dogLeg(c,16,-22+bounce,21,7,phase,false);
        c.save();c.translate((float)Math.sin(a)*.6f,bounce);
        path.reset();path.moveTo(-19,-14);path.cubicTo(-24,-23,-19,-36,-15,-46);
        path.cubicTo(-11,-59,12,-59,16,-46);path.cubicTo(20,-35,25,-24,20,-14);
        path.cubicTo(8,-7,-9,-7,-19,-14);path.close();
        p.setColor(Color.WHITE);p.setShader(dogCoat);c.drawPath(path,p);p.setShader(null);
        path.reset();path.moveTo(-9,-47);path.cubicTo(-2,-53,8,-53,10,-44);
        path.cubicTo(5,-33,12,-26,7,-16);path.cubicTo(-2,-12,-10,-24,-7,-32);
        path.close();p.setColor(0xffeef1eb);c.drawPath(path,p);
        oval(c,-21,-28,-7,-10,0xffdce4e7);oval(c,7,-28,22,-10,0xff425260);
        line(c,-12,-44,-16,-34,0xffb8c8d2,1.5f);line(c,13,-44,16,-34,0xffb8c8d2,1.5f);
        // Only the nape, ears and a small side profile are visible from the chase camera.
        oval(c,-13,-62,13,-43,0xffe7ece6);
        line(c,-12,-47,12,-47,0xff28b8ed,3.5f);
        circle(c,9,-47,1.7f,0xffd0ac64);
        c.save();c.translate(-2,-63);c.rotate((float)Math.sin(a)*1.3f);
        oval(c,-14,-12,14,12,0xff344452);
        poly(c,0xfff4f4ec,-4,-11,2,-12,5,5,-2,11,-6,3);
        poly(c,0xff1c2937,-12,-2,-19,-21-(float)Math.sin(a)*1.5f,-4,-11);
        poly(c,0xff263744,12,-2,19,-20+(float)Math.sin(a)*1.5f,4,-11);
        poly(c,0xff101b27,-11,-6,-15,-16,-5,-10);
        poly(c,0xff162635,11,-6,15,-15,6,-10);
        poly(c,0xfff1f2e9,-12,-10,-20,-6,-20,0,-10,4);
        oval(c,-22,-7,-17,-2,0xff343b2b);
        line(c,-15,-5,-14,-6,0xff403b2c,1);
        oval(c,-9,0,10,12,0xffdce4e7);c.restore();
        path.reset();path.moveTo(2,-16);path.cubicTo(9,-7,24+(float)Math.sin(a-.4f)*2,-6,
            27+(float)Math.sin(a-.4f)*3,-25);
        p.setColor(0xff243542);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(path,p);p.setStrokeWidth(1.4f);p.setColor(0xffe3eceb);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        c.restore();
        for(int i=0;i<2;i++) {
            float dust=RunCycle.cycle(phase+i*.5f);int alpha=(int)((1-dust)*35);
            circle(c,i==0?-20:20,10+dust*13,1+dust*3,(alpha<<24)|0xe4c28b);
        }
        c.restore();
    }
    private void dogLeg(Canvas c,float hx,float hy,float baseX,float floor,float phase,boolean front) {
        float swing=Math.max(0,(float)Math.sin(phase*RunCycle.TAU));
        float lift=swing*swing*13;
        float reach=(float)Math.cos(phase*RunCycle.TAU)*5;
        float px=baseX+reach*.12f,py=floor-lift;
        float kneeX=hx+(hx<0?-1:1)*(front?2:3),kneeY=hy+(front?12:8)+reach*.3f;
        float hockX=(kneeX+px)*.5f,hockY=(kneeY+py)*.5f-3;
        int color=front?0xff2b3c4a:0xff566875;
        line(c,hx,hy,kneeX,kneeY,color,front?6:9);
        line(c,kneeX,kneeY,hockX,hockY,color,front?5:6);
        line(c,hockX,hockY,px,py,front?0xffcdd9de:0xffe1e9e8,4);
        oval(c,px-4.5f,py-2,px+4.5f,py+3.5f,0xfff1f3ec);
        for(int i=-1;i<=1;i++)line(c,px+i*2,py+1,px+i*2,py+3,0xff475966,.7f);
    }

    private void drawMenu(Canvas c) {
        // A descending valley can move the skyline behind the title on short
        // screens. Fade the moving scenery beneath the lettering.
        float titleScale=Math.min(1,Math.max(.78f,(H-520)/360));
        float veilBottom=90+206*titleScale+62;
        int veil=daylight.skyTop & 0x00ffffff;
        p.setColor(Color.WHITE);
        p.setShader(new LinearGradient(0,0,0,veilBottom,
            new int[]{0xff000000|veil,0xf0000000|veil,veil},new float[]{0,.72f,1},Shader.TileMode.CLAMP));
        c.drawRect(0,0,W,veilBottom,p);p.setShader(null);
        brandIcon.setBounds(26,34,78,86);brandIcon.draw(c);
        text(c,"MADE TO ROLL",91,56,11,outsideText(),bold,Paint.Align.LEFT);
        text(c,"An Ethiopian village adventure",91,75,10,outsideText(),regular,Paint.Align.LEFT);
        soundButton(c,386,58);
        c.save();c.scale(titleScale,titleScale,216,90);
        text(c,"ቡቹ",216,194,108,0x25a66a30,bold,Paint.Align.CENTER);
        text(c,"ቡቹ",216,190,108,outsideText(),bold,Paint.Align.CENTER);
        text(c,"B U C H U",216,232,23,outsideText(),bold,Paint.Align.CENTER);
        text(c,"One tire. A whole village to outrun.",216,265,14,outsideText(),regular,Paint.Align.CENTER);
        // Woven details connect the title, the boy's shirt, and the icon palette.
        line(c,139,289,185,289,GREEN,2);line(c,247,289,293,289,GREEN,2);
        for(int i=-1;i<=1;i++) {
            float x=216+i*14;
            poly(c,i==0?GOLD:RUST,x,282,x+5,289,x,296,x-5,289);
        }
        c.restore();
        float top=H-226;
        round(c,18,top+5,414,H-21,28,0x3031261c);
        round(c,18,top,414,H-26,28,0xfffff6e1);
        text(c,"NEXT MISSION  ·  +25 COINS",216,top+22,10,GREEN,bold,Paint.Align.CENTER);
        text(c,GameEngine.missionLabel(missionsCompleted),216,top+40,12,INK,bold,Paint.Align.CENTER);
        round(c,34,top+51,210,top+88,13,0xffeee5cb);
        round(c,222,top+51,398,top+88,13,0xffeee5cb);
        text(c,"BEST SCORE",46,top+75,9,GREEN,bold,Paint.Align.LEFT);
        text(c,""+bestScore,198,top+76,15,INK,bold,Paint.Align.RIGHT);
        drawCoin(c,241,top+71,.52f,0);
        text(c,"COINS",258,top+75,10,GREEN,bold,Paint.Align.LEFT);
        text(c,""+bank,383,top+76,16,INK,bold,Paint.Align.RIGHT);
        button(c,34,top+100,398,top+158,"LET’S ROLL",GOLD,INK);
        poly(c,INK,105,top+118,105,top+139,120,top+128.5f);
        text(c,"SWIPE TO STEER  ·  SWIPE UP TO JUMP",216,top+184,10,GREEN,medium,Paint.Align.CENTER);
    }
    private void drawHud(Canvas c) {
        round(c,18,43,175,112,19,0xf2fff6e1);
        text(c,"VILLAGE RUN",33,65,10,GREEN,bold,Paint.Align.LEFT);
        text(c,(int)game.distance+"",33,96,29,INK,bold,Paint.Align.LEFT);
        text(c,"METRES",112,94,9,INK,medium,Paint.Align.LEFT);
        round(c,188,43,339,112,19,0xf2fff6e1);
        drawCoin(c,210,78,.65f,0);
        if(game.state==GameEngine.State.RUNNING && toastTime>0)text(c,"+1",316,88,12,GREEN,bold,Paint.Align.CENTER);
        text(c,String.format(java.util.Locale.US,"%03d",game.coins),229,91,29,INK,bold,Paint.Align.LEFT);
        round(c,353,43,414,112,19,INK);
        line(c,377,68,377,87,CREAM,4);line(c,390,68,390,87,CREAM,4);
        round(c,18,124,414,186,15,game.missionComplete?0xf2f5e4a7:0xf2fff6e1);
        text(c,game.missionComplete?"MISSION COMPLETE  ·  +25 COINS":"RUN MISSION  ·  +25 COINS",32,142,9,GREEN,bold,Paint.Align.LEFT);
        text(c,game.missionProgress()+" / "+game.missionTarget(),400,142,10,INK,bold,Paint.Align.RIGHT);
        text(c,game.missionLabel(),32,161,12,INK,bold,Paint.Align.LEFT);
        round(c,32,171,400,176,2,0xffded6ba);
        float progress=game.missionProgress()/(float)game.missionTarget();
        if(progress>0)round(c,32,171,32+368*progress,176,2,GREEN);
        GameEngine.Item surprise=game.approachingSurprise();
        if(surprise!=null) {
            round(c,34,194,398,239,12,0xfff8ce65);
            text(c,GameEngine.warningTitle(surprise.kind),216,212,12,INK,bold,Paint.Align.CENTER);
            text(c,GameEngine.warningHint(surprise),216,230,10,INK,bold,Paint.Align.CENTER);
        } else if(game.strikes>0) {
            round(c,45,194,387,236,12,0xf2fff6e1);
            text(c,"THE DOG IS GETTING CLOSER!",216,211,11,RUST,bold,Paint.Align.CENTER);
            text(c,"Stay clear for "+(int)Math.ceil(game.recovery)+"s to recover",216,227,10,INK,regular,Paint.Align.CENTER);
        } else if(game.downhillBoost()>.08f) {
            round(c,110,194,322,225,12,0xf2f5c149);
            int percent=Math.round((game.speed()/game.baseSpeed()-1)*100);
            text(c,"DOWNHILL BOOST  +"+percent+"%",216,214,11,INK,bold,Paint.Align.CENTER);
        } else text(c,district(),216,211,10,outsideText(),bold,Paint.Align.CENTER);
        if(skillToastTime>0) {
            round(c,74,246,358,280,12,0xf2244538);
            text(c,skillToast,216,268,13,CREAM,bold,Paint.Align.CENTER);
        }
        round(c,18,H-48,414,H-18,12,0xe6fff6e1);
        text(c,"SCORE  "+game.score(),32,H-28,11,INK,bold,Paint.Align.LEFT);
        text(c,"BEST  "+Math.max(bestScore,game.score()),400,H-28,10,GREEN,bold,Paint.Align.RIGHT);
        if(game.elapsed<6 && game.strikes==0) {
            round(c,34,H-105,398,H-57,14,0xf2fff6e1);
            text(c,"SWIPE TO STEER  ·  SWIPE UP TO HOP",216,H-85,11,INK,bold,Paint.Align.CENTER);
            text(c,"Dodge late or time a perfect hop for bonus points.",216,H-68,10,INK,regular,Paint.Align.CENTER);
        }
    }
    private String district() { return TrailCamera.district(game.distance); }
    private void drawPause(Canvas c) {
        box(c,0,0,W,H,0x9c20372e);
        float y=H*.5f-185;
        round(c,25,y,407,y+370,27,CREAM);
        text(c,"TAKE A BREATHER",216,y+47,22,INK,bold,Paint.Align.CENTER);
        text(c,"Your village adventure can wait.",216,y+75,13,INK,regular,Paint.Align.CENTER);
        text(c,"Swipe sideways to change lanes.",216,y+116,13,INK,regular,Paint.Align.CENTER);
        text(c,"Swipe up to jump. Steer around mud.",216,y+140,13,INK,regular,Paint.Align.CENTER);
        text(c,"Two hits within 8 seconds end the run.",216,y+164,13,INK,regular,Paint.Align.CENTER);
        button(c,45,y+190,387,y+245,"KEEP ROLLING",RUST,CREAM);
        button(c,45,y+257,387,y+307,"BACK TO VILLAGE",0xffe7ddc6,INK);
        text(c,"MUSIC & SOUND "+(sound?"ON":"OFF")+"  ·  TAP TO CHANGE",216,y+341,11,INK,bold,Paint.Align.CENTER);
    }
    private void drawGameOver(Canvas c) {
        box(c,0,0,W,H,0x9c20372e);
        float y=H*.5f-240;
        round(c,25,y,407,y+480,27,CREAM);
        text(c,game.endReason.contains("mud")?"STUCK IN THE MUD!":"CAUGHT UP!",216,y+43,26,INK,bold,Paint.Align.CENTER);
        text(c,game.endReason,216,y+68,12,INK,regular,Paint.Align.CENTER);
        text(c,""+game.score(),216,y+125,45,INK,bold,Paint.Align.CENTER);
        text(c,"RUN SCORE  ·  "+game.bonusPoints+" SKILL POINTS",216,y+145,10,GREEN,bold,Paint.Align.CENTER);
        text(c,""+(int)game.distance,123,y+188,30,INK,bold,Paint.Align.CENTER);
        text(c,"METRES",123,y+207,10,INK,bold,Paint.Align.CENTER);
        text(c,"+"+game.earnedCoins(),305,y+188,30,RUST,bold,Paint.Align.CENTER);
        text(c,"COINS EARNED",305,y+207,10,INK,bold,Paint.Align.CENTER);
        line(c,216,y+163,216,y+209,0xffd9cdb4,1);
        text(c,game.nearMisses+" CLOSE CALL"+(game.nearMisses==1?"":"S")+"  ·  "+game.perfectHops+" PERFECT HOP"+(game.perfectHops==1?"":"S"),216,y+236,11,INK,bold,Paint.Align.CENTER);
        text(c,game.missionComplete?"MISSION COMPLETE  ·  +25 COINS BANKED":"MISSION  "+game.missionProgress()+" / "+game.missionTarget()+"  ·  TRY AGAIN",216,y+264,11,GREEN,bold,Paint.Align.CENTER);
        text(c,"BEST SCORE  "+bestScore+"  ·  LONGEST  "+best+" m",216,y+292,10,GREEN,bold,Paint.Align.CENTER);
        button(c,45,y+317,387,y+371,"ONE MORE RUN",RUST,CREAM);
        button(c,45,y+384,387,y+432,"BACK TO VILLAGE",0xffe7ddc6,INK);
        text(c,game.missionComplete?"Your next mission is ready.":"A fresh run. Another chance.",216,y+458,11,INK,regular,Paint.Align.CENTER);
    }
    private void soundButton(Canvas c,float x,float y) { round(c,x-24,y-18,x+24,y+18,12,0xffe9ddbf);text(c,sound?"♪":"×",x,y+8,23,INK,bold,Paint.Align.CENTER); }
    private void button(Canvas c,float l,float t,float r,float b,String label,int bg,int fg) { round(c,l,t+4,r,b+4,16,0x20332920);round(c,l,t,r,b,16,bg);text(c,label,(l+r)/2,(t+b)/2+6,15,fg,bold,Paint.Align.CENTER); }
    private void text(Canvas c,String s,float x,float y,float size,int color,Typeface face,Paint.Align align) { p.setColor(color);p.setTextSize(size);p.setTypeface(face);p.setTextAlign(align);c.drawText(s,x,y,p); }
    private void box(Canvas c,float l,float t,float r,float b,int color) { p.setColor(color);c.drawRect(l,t,r,b,p); }
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color) { p.setColor(color);rect.set(l,t,r,b);c.drawRoundRect(rect,radius,radius,p); }
    private void circle(Canvas c,float x,float y,float radius,int color) { p.setColor(color);c.drawCircle(x,y,Math.max(0,radius),p); }
    private void oval(Canvas c,float l,float t,float r,float b,int color) { p.setColor(color);c.drawOval(l,t,r,b,p); }
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float width) { p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p); }
    private void poly(Canvas c,int color,float... points) { p.setColor(color);path.reset();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();c.drawPath(path,p); }

    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        float x=event.getX()/scale,y=event.getY()/scale;
        if(event.getAction()==MotionEvent.ACTION_DOWN) { downX=x;downY=y;return true; }
        if(event.getAction()==MotionEvent.ACTION_UP) {
            float dx=x-downX,dy=y-downY;
            if(game.state==GameEngine.State.RUNNING && Math.max(Math.abs(dx),Math.abs(dy))>25) {
                if(Math.abs(dx)>Math.abs(dy))game.move(dx>0?1:-1);else if(dy<0)jump();
            } else if(Math.max(Math.abs(dx),Math.abs(dy))<25) tap(x,y);
            performClick();invalidate();return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick();return true; }
    private void tap(float x,float y) {
        switch(game.state) {
            case MENU:
                if(x>355 && y<86)toggleSound();
                else if(y>H-126 && y<H-68 && x>34 && x<398)start();
                break;
            case RUNNING:
                if(x>350 && y>40 && y<115)game.pause();
                break;
            case PAUSED:
                float pY=H*.5f-185;
                if(x>45 && x<387) {
                    if(y>pY+190 && y<pY+245)resume();
                    else if(y>pY+257 && y<pY+307) { saveProgress();game.state=GameEngine.State.MENU; }
                    else if(y>pY+317 && y<pY+362)toggleSound();
                }
                break;
            case OVER:
                if(mudImpact>0)break;
                float oY=H*.5f-240;
                if(x>45 && x<387) {
                    if(y>oY+317 && y<oY+371)start();
                    else if(y>oY+384 && y<oY+432)game.state=GameEngine.State.MENU;
                }
                break;
        }
    }
    private void start() { runPhase=0;dogPhase=0;mudImpact=0;resetCamera=true;saved=false;toastTime=skillToastTime=0;game.start(missionsCompleted);lastFrame=0;invalidate(); }
    private void resume() { game.resume();lastFrame=0;invalidate(); }
    private void jump() {
        if(game.state==GameEngine.State.RUNNING && game.jumpTime<=0) { game.jump();audio.play(GameAudio.JUMP); }
    }
    private void toggleSound() { sound=!sound;prefs.edit().putBoolean("sound",sound).apply();audio.update(sound,active,game.state); }
    @Override public boolean onKeyDown(int key,android.view.KeyEvent event) {
        if(event.getRepeatCount()>0)return true;
        if(key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_A)game.move(-1);
        else if(key==KeyEvent.KEYCODE_DPAD_RIGHT || key==KeyEvent.KEYCODE_D)game.move(1);
        else if(key==KeyEvent.KEYCODE_DPAD_UP || key==KeyEvent.KEYCODE_W || key==KeyEvent.KEYCODE_SPACE) { if(game.state==GameEngine.State.MENU || game.state==GameEngine.State.OVER)start();else jump(); }
        else if(key==KeyEvent.KEYCODE_P || key==KeyEvent.KEYCODE_ESCAPE)back();
        else if(key==KeyEvent.KEYCODE_ENTER) { if(game.state==GameEngine.State.PAUSED)resume();else if(game.state!=GameEngine.State.RUNNING)start(); }
        else return super.onKeyDown(key,event);
        invalidate();return true;
    }
    public void back() { if(game.state==GameEngine.State.RUNNING)game.pause();else if(game.state==GameEngine.State.PAUSED)resume();else game.state=GameEngine.State.MENU;invalidate(); }
    public void background() { game.pause();active=false;lastFrame=0;audio.update(sound,false,game.state); }
    public void foreground() { active=true;lastFrame=0;audio.update(sound,true,game.state);invalidate(); }
    public void release() { audio.release(); }
    @Override public void warning(GameEngine.Kind kind) { audio.play(GameAudio.WARNING); }
    @Override public void coin() { audio.play(GameAudio.COIN);toastTime=.6f; }
    @Override public void skill(String label,int points) {
        skillToast=label+"  +"+points;skillToastTime=1.6f;
        audio.play(GameAudio.SKILL);
    }
    @Override public void missionComplete() {
        // Bank mission rewards immediately; ordinary run coins are saved at
        // game over / return to the village and must not include this bonus twice.
        missionsCompleted++;bank+=GameEngine.MISSION_REWARD;
        prefs.edit().putInt("missionsCompleted",missionsCompleted).putInt("bank",bank).apply();
        skillToast="MISSION COMPLETE!  +25 COINS";skillToastTime=2.8f;
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        audio.play(GameAudio.MISSION);
    }
    @Override public void stumble() { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);audio.play(GameAudio.STUMBLE); }
    @Override public void end(String reason) { mudImpact=reason.contains("mud")?.75f:0;saveProgress();performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);audio.play(GameAudio.END); }
    private void saveProgress() { if(saved)return;saved=true;best=Math.max(best,(int)game.distance);bestScore=Math.max(bestScore,game.score());bank+=game.coins;prefs.edit().putInt("best",best).putInt("bestScore",bestScore).putInt("bank",bank).apply(); }
}
