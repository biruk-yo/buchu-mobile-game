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
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Activity activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
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
                    float[] distances={30,220,430};String[] places={"woodland","compounds","grove"};
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
                    view.game.state=GameEngine.State.MENU;set(view,"active",true);set(view,"animation",0f);view.invalidate();
                } catch(Exception e) {throw new RuntimeException(e);}
            });
            result.putString("stream","Visual proof: lighting, village districts, stride phases and obstacle passing saved.\n");finish(Activity.RESULT_OK,result);
        } catch(Exception e) {result.putString("stream",e.toString());finish(Activity.RESULT_CANCELED,result);}
    }
}
