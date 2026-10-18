package dev.shtark.rpganimation.animation;

public final class AnimationTempoTest {
    public static void run() {
        check(AnimationTempo.wake(0)==0 && AnimationTempo.wake(96)==120, "Wake endpoints");
        check(AnimationTempo.entry(0)==0 && AnimationTempo.entry(64)==88, "Bed endpoints");
        double previousWake=0,previousBed=0;
        for(double time=0;time<=100;time+=.05) {
            double wake=AnimationTempo.wake(time),bed=AnimationTempo.entry(time);
            check(wake>=previousWake && bed>=previousBed && wake-previousWake<.2 && bed-previousBed<.2,
                    "Monotonic continuous phase");
            var frame=WakeUpSequence.sample(time,false);
            var pose=GetUpSequence.sample(wake);
            check(Math.abs(frame.eyeFraction()-pose.eyeFraction())<1e-8,"Camera and body share phase");
            var rub=EyeRubSequence.sample(time,false);
            check(rub.equals(EyeRubSequence.samplePhase(wake,false)),"Hand shares blink/body phase");
            previousWake=wake;previousBed=bed;
        }
        for(double time:new double[]{22,40}) {
            check(WakeUpSequence.sample(time,false).eyelids()==1
                    && EyeRubSequence.sample(time,false).horizontal()==.045,"Both blinks coincide with rub peak");
        }
        check(AnimationTempo.wake(23)-AnimationTempo.wake(22)>2*(AnimationTempo.wake(90)-AnimationTempo.wake(89)),
                "Active gesture faster than final settle");
        check(AnimationTempo.entry(20)-AnimationTempo.entry(19)>2*(AnimationTempo.entry(61)-AnimationTempo.entry(60)),
                "Bed climb faster than final settle");
        check(WakeUpSequence.sample(96,false).eyeFraction()==1 && EyeRubSequence.sample(96,false).lift()==0,
                "New expiry fully neutral");
        System.out.println("Passed: shared nonuniform tempo, synchronized camera/body/rub/blinks and neutral shortened endpoints");
    }
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
}
