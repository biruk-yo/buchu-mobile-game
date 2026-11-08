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
                    float[] times={0,28.8f,51.3f,73.8f};String[] names={"day.png","sunset.png","night.png","sunrise.png"};
                    for(int i=0;i<times.length;i++) {
                        set(view,"animation",times[i]);set(view,"runPhase",.32f);set(view,"dogPhase",.32f);
                        canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();save(frame,names[i]);
                    }
                    Bitmap sheet=Bitmap.createBitmap(864,1008,Bitmap.Config.ARGB_8888);Canvas cells=new Canvas(sheet);
                    Paint paint=new Paint(3);paint.setColor(0xff21372f);paint.setTextSize(12);
                    set(view,"animation",0f);
                    for(int i=0;i<16;i++) {
                        set(view,"runPhase",i/16f);set(view,"dogPhase",i/16f);
                        canvas.save();canvas.scale(432f/view.getWidth(),960f/view.getHeight());view.draw(canvas);canvas.restore();
                        int x=(i%4)*216,y=(i/4)*252;
                        cells.drawBitmap(frame,new Rect(130,580,365,858),new Rect(x,y,x+216,y+252),paint);
                        cells.drawText(""+i,x+6,y+15,paint);
                    }
                    save(sheet,"stride-sheet.png");sheet.recycle();frame.recycle();
                    view.game.state=GameEngine.State.MENU;set(view,"active",true);set(view,"animation",0f);view.invalidate();
                } catch(Exception e) {throw new RuntimeException(e);}
            });
            result.putString("stream","Visual proof: day, sunset, night, sunrise and 16 stride phases saved.\n");finish(Activity.RESULT_OK,result);
        } catch(Exception e) {result.putString("stream",e.toString());finish(Activity.RESULT_CANCELED,result);}
    }
}
