package com.tirerun.game;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

/** Produces actual Android-rendered lighting and animation proof sheets, without release hooks. */
public final class VisualCheck extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args);start(); }
    private static void set(Object o,String name,Object value) throws Exception {
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);
    }
    private void save(Bitmap bitmap,String name) throws Exception {
        File dir=new File(getTargetContext().getFilesDir(),"visual-check");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))) {bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
    private static void tap(GameView view,float x,float y) {
        float scale=view.getWidth()/432f;
        long now=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,x*scale,y*scale,0);
        android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+30,android.view.MotionEvent.ACTION_UP,x*scale,y*scale,0);
        view.onTouchEvent(down);view.onTouchEvent(up);down.recycle();up.recycle();
    }
    private void rewardFrame(GameView view,Bitmap frame,String name) throws Exception {
        Canvas canvas=new Canvas(frame);view.draw(canvas);save(frame,name+".png");
    }
    private void verifyRewards() throws Exception {
        android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("tire_run",0);
        int initialBank=prefs.getInt("bank",0),initialScore=prefs.getInt("bestScore",0);
        GameView view=new GameView(getTargetContext());set(view,"active",false);set(view,"sound",false);
        set(view,"missionsCompleted",0);view.layout(0,0,432,768);
        Bitmap frame=Bitmap.createBitmap(432,768,Bitmap.Config.ARGB_8888);view.draw(new Canvas(frame));
        tap(view,216,671);view.game.items.clear();view.game.distance=150;view.game.elapsed=8;
        set(view,"animation",10f);set(view,"runPhase",.32f);set(view,"resetCamera",true);
        rewardFrame(view,frame,"downhill-boost");
        view.game.items.add(new GameEngine.Item(GameEngine.Kind.ROCK,0,4));
        view.game.update(.01f);view.game.move(1);view.game.update(.25f);
        if(view.game.nearMisses!=1)throw new IllegalStateException("Close-call input path failed");
        rewardFrame(view,frame,"close-call");
        view.game.items.clear();view.game.move(-1);view.game.update(.25f);
        view.game.jumpTime=.425f;view.game.items.add(new GameEngine.Item(GameEngine.Kind.LOG,0,.01f));view.game.update(.01f);
        if(view.game.perfectHops!=1)throw new IllegalStateException("Perfect-hop input path failed");
        rewardFrame(view,frame,"perfect-hop");
        view.game.coins=11;view.game.items.add(new GameEngine.Item(GameEngine.Kind.COIN,0,.01f));view.game.update(.01f);
        if(!view.game.missionComplete || prefs.getInt("bank",0)!=initialBank+25 || prefs.getInt("missionsCompleted",0)!=1)
            throw new IllegalStateException("Mission reward was not banked immediately");
        rewardFrame(view,frame,"mission-complete");
        int expectedScore=Math.max(initialScore,view.game.score());
        view.game.items.add(new GameEngine.Item(GameEngine.Kind.MUD,0,.01f));view.game.update(.01f);set(view,"mudImpact",0f);
        if(prefs.getInt("bank",0)!=initialBank+37 || prefs.getInt("bestScore",0)<expectedScore)
            throw new IllegalStateException("Run score or earned coins were not saved");
        rewardFrame(view,frame,"rewards-result");
        view.end("The tire got stuck in the mud.");set(view,"mudImpact",0f);
        if(prefs.getInt("bank",0)!=initialBank+37)throw new IllegalStateException("Run rewards were banked twice");
        tap(view,216,488);
        if(view.game.state!=GameEngine.State.RUNNING || view.game.missionTarget()!=3 || view.game.bonusPoints!=0)
            throw new IllegalStateException("Retry button did not start the next mission");
        view.game.items.clear();view.game.items.add(new GameEngine.Item(GameEngine.Kind.MUD,0,.01f));view.game.update(.01f);set(view,"mudImpact",0f);
        tap(view,216,552);
        if(view.game.state!=GameEngine.State.MENU || prefs.getInt("bank",0)!=initialBank+37)
            throw new IllegalStateException("Return to village changed rewards");
        view.release();
        GameView reloaded=new GameView(getTargetContext());set(reloaded,"active",false);reloaded.layout(0,0,432,768);
        rewardFrame(reloaded,frame,"next-mission");tap(reloaded,216,671);
        if(reloaded.game.missionTarget()!=3)throw new IllegalStateException("Completed mission did not survive view recreation");
        reloaded.release();frame.recycle();
    }
    private void verifyAudio() {
        GameAudio[] audio={null};boolean[] ready={false};
        runOnMainSync(() -> audio[0]=new GameAudio(getTargetContext()));
        try {
            for(int i=0;i<40 && !ready[0];i++) {
                android.os.SystemClock.sleep(50);
                runOnMainSync(() -> ready[0]=audio[0].areEffectsReady());
            }
            runOnMainSync(() -> {
                if(!ready[0])throw new IllegalStateException("Bundled sound cues failed to load");
                audio[0].update(true,true,GameEngine.State.RUNNING);
                if(!audio[0].isMusicPlaying())throw new IllegalStateException("Music did not start");
                audio[0].update(true,true,GameEngine.State.PAUSED);
                if(audio[0].isMusicPlaying())throw new IllegalStateException("Pause left music playing");
                audio[0].update(false,true,GameEngine.State.RUNNING);
                if(audio[0].isMusicPlaying())throw new IllegalStateException("Mute left music playing");
                audio[0].update(true,true,GameEngine.State.MENU);
                if(!audio[0].isMusicPlaying())throw new IllegalStateException("Unmute did not restart music");
                audio[0].update(true,false,GameEngine.State.RUNNING);
                if(audio[0].isMusicPlaying())throw new IllegalStateException("Background left music playing");
            });
        } finally {runOnMainSync(() -> audio[0].release());}
    }
    private void verifyVillage() throws Exception {
        GameView view=new GameView(getTargetContext());set(view,"active",false);set(view,"sound",false);
        view.layout(0,0,432,768);view.game.start();view.game.distance=220;view.game.elapsed=20;
        set(view,"resetCamera",true);set(view,"animation",12f);
        Bitmap frame=Bitmap.createBitmap(432,768,Bitmap.Config.ARGB_8888);
        GameEngine.Kind[] kinds={GameEngine.Kind.CHICKEN,GameEngine.Kind.BASKET,GameEngine.Kind.CART};
        String[] names={"surprise-chickens","surprise-basket","surprise-cart"};
        for(int i=0;i<kinds.length;i++) {
            view.game.items.clear();GameEngine.Item surprise=new GameEngine.Item(kinds[i],i-1,7);
            surprise.warned=true;surprise.warningAge=3;view.game.items.add(surprise);
            rewardFrame(view,frame,names[i]);
        }
        view.game.items.clear();GameEngine.Item warning=new GameEngine.Item(GameEngine.Kind.CHICKEN,0,80);
        warning.warned=true;warning.warningAge=.5f;view.game.items.add(warning);
        rewardFrame(view,frame,"surprise-warning");
        set(view,"animation",DayNightCycle.SECONDS*.57f);warning.z=12;
        rewardFrame(view,frame,"surprise-night");
        view.release();frame.recycle();
        Bitmap homes=Bitmap.createBitmap(864,450,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(homes);
        c.drawColor(0xffd8e2cc);Paint paint=new Paint(3);paint.setColor(0xff718555);c.drawRect(0,330,864,450,paint);
        VillageHomes art=new VillageHomes();
        for(int i=0;i<3;i++)art.draw(c,140+i*285,365,1.8f,i,12,0);
        paint.setColor(0xff253e33);paint.setTextSize(18);paint.setTextAlign(Paint.Align.CENTER);
        c.drawText("THATCH, PLASTER, TIMBER & CORRUGATED ROOFS",432,45,paint);
        save(homes,"village-homes.png");homes.recycle();
    }
    @Override public void onStart() {
        Bundle result=new Bundle();int resultCode=Activity.RESULT_CANCELED;
        android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("tire_run",0);
        java.util.Map<String,?> original=new java.util.HashMap<>(prefs.getAll());
        try {
            Activity activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            verifyAudio();
            runOnMainSync(() -> {
                try {
                    Field f=MainActivity.class.getDeclaredField("game");f.setAccessible(true);GameView view=(GameView)f.get(activity);
                    set(view,"active",false);view.game.start();view.game.distance=155;view.game.elapsed=15;
                    view.game.items.clear();view.game.items.add(new GameEngine.Item(GameEngine.Kind.MUD,-1,13));
                    view.game.items.add(new GameEngine.Item(GameEngine.Kind.ROCK,1,21));
                    view.game.items.add(new GameEngine.Item(GameEngine.Kind.COIN,0,12));
                    Bitmap frame=Bitmap.createBitmap(432,960,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(frame);
                    float[] times={0,DayNightCycle.SECONDS*.32f,DayNightCycle.SECONDS*.57f,DayNightCycle.SECONDS*.82f};
                    String[] names={"day.png","sunset.png","night.png","sunrise.png"};
                    for(int i=0;i<times.length;i++) {
                        set(view,"animation",times[i]);set(view,"runPhase",.32f);set(view,"dogPhase",.32f);
                        canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();save(frame,names[i]);
                    }
                    Bitmap sheet=Bitmap.createBitmap(864,1008,Bitmap.Config.ARGB_8888);Canvas cells=new Canvas(sheet);
                    Paint paint=new Paint(3);paint.setColor(0xff21372f);paint.setTextSize(12);
                    set(view,"animation",0f);
                    for(int i=0;i<16;i++) {
                        // Advance travel as well as the gait, so the proof sheet
                        // shows tread rotation independently of the hand strokes.
                        view.game.distance=155+i*.45f;set(view,"resetCamera",true);
                        set(view,"runPhase",i/16f);set(view,"dogPhase",i/16f);
                        canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                        int x=(i%4)*216,y=(i/4)*252;
                        cells.drawBitmap(frame,new Rect(130,580,365,858),new Rect(x,y,x+216,y+252),paint);
                        cells.drawText(""+i,x+6,y+15,paint);
                    }
                    save(sheet,"stride-sheet.png");sheet.recycle();
                    float[] distances={30,220,430,0,80,170,265,350};String[] places={"woodland","compounds","grove","hill-climb","hill-crest","hill-descent","valley","hill-return"};
                    for(int i=0;i<distances.length;i++) {
                        view.game.distance=distances[i];set(view,"resetCamera",true);
                        canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                        save(frame,places[i]+".png");
                    }
                    // Follow each obstacle past the collision plane to the bottom
                    // edge. Resolved hazards must remain visible in these frames.
                    Bitmap passing=Bitmap.createBitmap(1296,1920,Bitmap.Config.ARGB_8888);
                    Canvas passingCanvas=new Canvas(passing);
                    float[] depths={1,-.2f,-2,-4,-6,-11};
                    GameEngine.Kind[] kinds={GameEngine.Kind.ROCK,GameEngine.Kind.LOG,GameEngine.Kind.MUD,GameEngine.Kind.COIN};
                    view.game.distance=155;set(view,"runPhase",.32f);set(view,"dogPhase",.32f);
                    for(int row=0;row<kinds.length;row++)for(int col=0;col<depths.length;col++) {
                        view.game.items.clear();
                        GameEngine.Item item=new GameEngine.Item(kinds[row],0,depths[col]);
                        item.resolved=depths[col]<=0;view.game.items.add(item);set(view,"resetCamera",true);
                        canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                        int x=col*216,y=row*480;
                        passingCanvas.drawBitmap(frame,null,new Rect(x,y,x+216,y+480),paint);
                        passingCanvas.drawText(kinds[row]+" z="+depths[col],x+8,y+100,paint);
                    }
                    save(passing,"obstacle-passing.png");passing.recycle();
                    view.game.state=GameEngine.State.MENU;set(view,"animation",0f);
                    canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                    save(frame,"start-screen.png");
                    set(view,"animation",DayNightCycle.SECONDS*.57f);
                    canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                    save(frame,"start-screen-night.png");
                    // Follow the actual pause-menu return path, including a stale
                    // stumble and jump that must not leak into the village preview.
                    view.game.start();view.game.distance=220;view.game.strikes=1;
                    view.game.flash=.4f;view.game.jumpTime=.3f;set(view,"animation",0f);
                    canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                    view.game.pause();
                    float logicalHeight=view.getHeight()/(view.getWidth()/432f);
                    tap(view,216,logicalHeight*.5f-185+282);
                    if(view.game.state!=GameEngine.State.MENU)throw new IllegalStateException("Back to village failed");
                    set(view,"active",true);
                    for(int i=0;i<3;i++) {
                        set(view,"lastFrame",System.nanoTime()-20_000_000L);
                        canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                    }
                    set(view,"active",false);
                    if(view.game.distance!=220 || view.game.coins!=0)throw new IllegalStateException("Village preview advanced the run");
                    save(frame,"back-to-village.png");frame.recycle();
                    Bitmap icon=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
                    android.graphics.drawable.Drawable drawable=getTargetContext().getPackageManager().getApplicationIcon(getTargetContext().getApplicationInfo());
                    drawable.setBounds(0,0,256,256);drawable.draw(new Canvas(icon));save(icon,"app-icon.png");icon.recycle();
                    GameView compact=new GameView(getTargetContext());set(compact,"active",false);
                    compact.layout(0,0,432,768);
                    Bitmap compactFrame=Bitmap.createBitmap(432,768,Bitmap.Config.ARGB_8888);
                    compact.draw(new Canvas(compactFrame));save(compactFrame,"start-screen-compact.png");compactFrame.recycle();
                    // Exercise the same touch coordinates used by the visible start button.
                    tap(compact,216,671);
                    if(compact.game.state!=GameEngine.State.RUNNING)throw new IllegalStateException("Start button does not start the game");
                    compact.release();
                    verifyRewards();
                    verifyVillage();
                    view.game.state=GameEngine.State.MENU;set(view,"active",true);set(view,"animation",0f);view.invalidate();
                } catch(Exception e) {throw new RuntimeException(e);}
            });
            result.putString("stream","Visual proof: village art, surprises, audio lifecycle, hills, rewards and persistence passed.\n");resultCode=Activity.RESULT_OK;
        } catch(Exception e) {result.putString("stream",e.toString());}
        finally {
            // Proof runs must not replace the player's bank, records or mission progression.
            android.content.SharedPreferences.Editor editor=prefs.edit().clear();
            for(java.util.Map.Entry<String,?> entry:original.entrySet()) {
                Object value=entry.getValue();String key=entry.getKey();
                if(value instanceof Integer)editor.putInt(key,(Integer)value);
                else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
                else if(value instanceof Long)editor.putLong(key,(Long)value);
                else if(value instanceof Float)editor.putFloat(key,(Float)value);
                else if(value instanceof String)editor.putString(key,(String)value);
            }
            editor.commit();
        }
        finish(resultCode,result);
    }
}
