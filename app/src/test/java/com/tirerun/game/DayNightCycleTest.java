package com.tirerun.game;

public final class DayNightCycleTest {
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    public static void main(String[] args) {
        DayNightCycle sky=new DayNightCycle();
        sky.sample(0);float initial=sky.sunX;int color=sky.skyTop;
        check(sky.daylight>.99f,"Game starts during daylight");
        sky.sample(12);check(sky.sunX>initial,"Sun travels across the sky");
        sky.sample(28.8f);check(sky.twilight>.99f,"Sunset has warm twilight");
        sky.sample(51.3f);check(sky.night>.99f && sky.moonHeight>.99f,"Night has a high moon");
        check((sky.worldTint>>>24)<=120,"Night tint preserves hazard visibility");
        sky.sample(73.8f);check(sky.twilight>.99f,"Sunrise returns");
        sky.sample(90);check(Math.abs(sky.sunX-initial)<.0001 && sky.skyTop==color,"Full cycle loops without a seam");
        float previous=0;
        for(int i=0;i<=900;i++) {
            sky.sample(i*.1f);
            check(Math.abs(sky.sunHeight+sky.moonHeight)<.0001,"Moon is opposite sun");
            if(i>0)check(Math.abs(sky.daylight-previous)<.03f,"Transitions are gradual");
            previous=sky.daylight;
        }
        System.out.println("PASS: moving sun, sunset/night/sunrise, readable night tint and seamless day cycle.");
    }
}
