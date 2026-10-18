package dev.shtark.rpganimation.animation;

public final class FishingIdleTest {
    public static void run() {
        var idle=new IdleSchedule(42);
        var other=new IdleSchedule(97);
        int count=0,different=0;
        for(int t=1;t<=10000;t++) {
            var actual=idle.tick(true);
            if(actual!=null)count++;
            if(actual!=other.tick(true))different++;
        }
        check(count>=6,"Repeated bounded idle stages");
        check(different>6,"Players do not share a fixed stretch clock");
        check(idle.tick(false)==null,"Activity clears pending stretch");
        for(int t=0;t<479;t++)check(idle.tick(true)==null,"Full minimum quiet interval after activity");
        check(ContextPolicy.mayStart(true,100,0,ContextClip.STRETCH_LARGE,ContextClip.FISH_CAST),"Cast overrides stretch");
        check(ContextPolicy.mayStart(true,100,0,ContextClip.FISH_WAIT,ContextClip.FISH_BITE),"Bite overrides waiting");
        check(!ContextPolicy.mayStart(true,100,0,ContextClip.FISH_REEL,ContextClip.STRETCH_SMALL),"Idle cannot replace reel");
        for(var clip:ContextClip.values())if(clip.fishing()||clip.stretch()) {
            double h=.001;
            check(Math.abs(clip.sample(h,0).mainX()/h)<.02,"Soft neutral start "+clip);
            check(Math.abs(clip.sample(clip.duration-h,0).mainX()/h)<.02,"Soft neutral return "+clip);
            check(clip.duration<=WakeUpSequence.DURATION,"Playback clock covers entire clip "+clip);
        }
        check(ContextClip.STRETCH_LARGE.sample(26,0).mainX()<ContextClip.STRETCH_SMALL.sample(13,0).mainX()-1,
                "Major stretch has distinct overhead reach");
        System.out.println("Passed: idle thresholds, reset/no backlog, fishing priorities, neutral velocities and distinct stretch intensity");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}

