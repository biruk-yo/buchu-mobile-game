package com.tirerun.game;

import android.content.Context;
import android.media.*;

/** Bundled instrumental music and short cues, respecting focus, mute and lifecycle. */
public final class GameAudio {
    public static final int COIN=0,JUMP=1,SKILL=2,MISSION=3,WARNING=4,STUMBLE=5,END=6;
    private final AudioManager manager;
    private final AudioFocusRequest focusRequest;
    private final SoundPool effects;
    private final int[] cues=new int[7];
    private final java.util.HashSet<Integer> loaded=new java.util.HashSet<>();
    private MediaPlayer music;
    private boolean wantsMusic,focused,enabled,active,released,ducked;
    public GameAudio(Context context) {
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener(this::focusChanged).build();
        effects=new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build();
        effects.setOnLoadCompleteListener((pool,id,status) -> { if(!released && status==0)loaded.add(id); });
        int[] resources={R.raw.coin,R.raw.jump,R.raw.skill,R.raw.mission,R.raw.warning,R.raw.stumble,R.raw.run_end};
        for(int i=0;i<cues.length;i++)cues[i]=effects.load(context,resources[i],1);
        try {
            music=MediaPlayer.create(context,R.raw.village_loop,attributes,0);
            if(music!=null) { music.setLooping(true);music.setVolume(.27f,.27f); }
        } catch(RuntimeException ignored) { music=null; }
    }
    public void update(boolean sound,boolean foreground,GameEngine.State state) {
        if(released)return;
        enabled=sound;active=foreground;
        boolean wanted=enabled && active && (state==GameEngine.State.MENU || state==GameEngine.State.RUNNING);
        if(!enabled || !active || state==GameEngine.State.PAUSED)effects.autoPause();
        if(wanted==wantsMusic)return;
        wantsMusic=wanted;
        if(wanted) {
            focused=manager!=null && manager.requestAudioFocus(focusRequest)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if(focused) { ducked=false;effects.autoResume();if(music!=null) {music.setVolume(.27f,.27f);music.start();} }
        } else {
            if(music!=null && music.isPlaying())music.pause();
            if(manager!=null && focused)manager.abandonAudioFocusRequest(focusRequest);
            focused=false;
        }
    }
    private void focusChanged(int change) {
        if(released)return;
        if(change==AudioManager.AUDIOFOCUS_GAIN) {
            focused=true;ducked=false;if(music!=null)music.setVolume(.27f,.27f);
            if(wantsMusic) { effects.autoResume();if(music!=null)music.start(); }
        } else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            ducked=true;
            if(music!=null)music.setVolume(.07f,.07f);
        } else {
            focused=false;effects.autoPause();
            if(music!=null && music.isPlaying())music.pause();
        }
    }
    public void play(int cue) {
        if(!released && enabled && active && focused && loaded.contains(cues[cue])) {
            float volume=(cue==WARNING?.7f:.5f)*(ducked?.25f:1);
            effects.play(cues[cue],volume,volume,cue==WARNING?2:1,0,1);
        }
    }
    boolean isMusicPlaying() { return !released && music!=null && music.isPlaying(); }
    boolean areEffectsReady() { return !released && loaded.size()==cues.length; }
    public void release() {
        if(released)return;released=true;
        if(manager!=null && focused)manager.abandonAudioFocusRequest(focusRequest);
        if(music!=null) { music.release();music=null; }
        effects.release();loaded.clear();
    }
}
