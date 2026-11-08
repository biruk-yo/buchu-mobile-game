package com.tirerun.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.View;

public class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        game = new GameView(this);
        setContentView(game);
        immersive();
    }
    private void immersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController control = getWindow().getInsetsController();
            if (control != null) {
                control.hide(WindowInsets.Type.systemBars());
                control.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else getWindow().getDecorView().setSystemUiVisibility(5894);
    }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if(focus) immersive(); }
    @Override protected void onPause() { super.onPause(); game.background(); }
    @Override protected void onResume() { super.onResume(); if(game != null) game.foreground(); }
    @Override protected void onDestroy() { game.release(); super.onDestroy(); }
    @Override public void onBackPressed() { game.back(); }
}
