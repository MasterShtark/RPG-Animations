package dev.shtark.rpganimation.animation;

public final class WakeUpSequenceTest {
    public static void main(String[] args) {
        check(WakeUpSequence.handLowering(0)==1,"Hands remain lowered before recovery");
        check(WakeUpSequence.handLowering(WakeUpSequence.DURATION)==0,"Hands neutral at completion");
        double previous=1;
        for(double t=0;t<=WakeUpSequence.DURATION+1;t+=.01) {
            double next=WakeUpSequence.handLowering(t);
            check(next>=0&&next<=previous&&previous-next<.002,"Hand return monotonic and continuous");
            previous=next;
        }
        check(WakeUpSequence.handLowering(WakeUpSequence.DURATION-.001)<.000001,"No endpoint pop");
        var rate=new PreviewCadence();
        check(rate.acquire(0,40)&&rate.acquire(0,40),"Preview allows bounded opening burst");
        check(!rate.acquire(0,40)&&!rate.acquire(-1,40),"No third burst or backwards-clock credit");
        check(!rate.acquire(39,40)&&rate.acquire(40,40),"Preview refills at configured rate");
        int accepted=0;rate.reset();
        for(int tick=0;tick<=400;tick++)for(int attempt=0;attempt<10;attempt++)if(rate.acquire(tick,40))accepted++;
        check(accepted==12,"Burst cannot increase sustained capture/upload rate");
        for(var clip:ContextClip.values()) {
            var a=clip.sample(clip.duration*.35,0);var b=ContextClip.FISH_REEL.sample(8,0);
            check(ContextClip.blend(a,b,0).equals(a),"Replacement begins at previous pose");
            check(ContextClip.blend(a,b,1).equals(b),"Replacement reaches new pose");
            check(ContextClip.blend(a,b,-1).equals(a),"Blend clamps negative progress");
        }
        BackpackIntegrationTest.run();
        ExpandedActionTest.run();
        BasicActionTest.run();
        AnimationOwnershipTest.run();
        FrameCacheTest.run();
        MultiplayerTimelineTest.run();
        EyeRubSequenceTest.run();
        GetUpSequenceTest.run();
        BedEntrySequenceTest.run();
        BedExitSequenceTest.run();
        DropSequenceTest.run();
        PickupSequenceTest.run();
        AnimationTempoTest.run();
        ContextClipTest.run();
        FishingIdleTest.run();
        ActivityTest.run();
        var first = WakeUpSequence.samplePhase(0, false);
        check(first.eyeFraction() < 0.2 && first.darkness() == 1 && first.eyelids() == 1, "Start low and dark");
        var last = WakeUpSequence.samplePhase(120, false);
        check(last.eyeFraction() == 1 && last.darkness() == 0 && last.roll() == 0 && last.pitchOffset() == 0 && last.eyelids() == 0, "Finish neutral");
        for (int height : new int[]{241, 360, 721}) {
            for (int step : new int[]{3, 6, 12}) {
                for (double x = -1; x <= 1; x += .1) {
                    check(WakeUpSequence.blockyLidHeight(x, 0, height, step) == 0, "Open blocky lids clear");
                    check(2 * WakeUpSequence.blockyLidHeight(x, 1, height, step) >= height, "Closed blocky lids leave no seam");
                    for (double closure = .01; closure < 1; closure += .03) {
                        int inset = WakeUpSequence.blockyLidHeight(x, closure, height, step);
                        check(inset >= 0 && inset <= (height + 1) / 2, "Blocky mask bounded");
                        check(inset == (height + 1) / 2 || inset % step == 0, "Whole-pixel block steps");
                    }
                }
            }
        }
        int blinks = 0;
        float previousLids = 1;
        double previousEye = 0;
        double previousDarkness = 1;
        for (double tick = -5; tick <= 130; tick += 0.25) {
            var frame = WakeUpSequence.samplePhase(tick, false);
            check(frame.eyelids() >= 0 && frame.eyelids() <= 1, "Bounded eyelids");
            check(Math.abs(frame.eyelids() - previousLids) < 0.05, "Continuous blink transitions");
            if (previousLids < 1 && frame.eyelids() == 1) blinks++;
            previousLids = frame.eyelids();
            check(frame.eyeFraction() >= previousEye && frame.eyeFraction() <= 1, "Monotonic rise");
            check(frame.darkness() <= previousDarkness && frame.darkness() >= 0, "No flashing");
            previousEye = frame.eyeFraction();
            previousDarkness = frame.darkness();
            var reduced = WakeUpSequence.samplePhase(tick, true);
            check(reduced.eyeFraction() == 1 && reduced.roll() == 0 && reduced.pitchOffset() == 0
                    && reduced.eyelids() == 0, "Reduced motion stays neutral without blinks");
        }
        check(blinks == 2, "Exactly two closures after opening");
        check(WakeUpSequence.samplePhase(32, false).eyelids() == 1, "First blink closes at 1.6s");
        check(WakeUpSequence.samplePhase(60, false).eyelids() == 1, "Second blink closes at 3s");
        check(WakeUpSequence.samplePhase(82, false).eyelids() == 0, "Fully open at 4.1s");
        for (double x = -1; x <= 1; x += 0.01) {
            check(WakeUpSequence.lidInset(x, 0) == 0, "No mask when open");
            check(Math.abs(WakeUpSequence.lidInset(x, 1) - 0.5) < 1e-9, "Closed lids cover screen");
            for (double closure = 0; closure <= 1; closure += 0.05) {
                double inset = WakeUpSequence.lidInset(x, closure);
                check(inset >= 0 && inset <= 0.5, "Mask within screen");
                check(Math.abs(inset - WakeUpSequence.lidInset(-x, closure)) < 1e-9, "Symmetric eyelids");
            }
        }
        check(WakeUpSequence.lidInset(1, 0.5) > WakeUpSequence.lidInset(0, 0.5), "Curved eye corners");
        check(WakeUpSequence.samplePhase(-100, false).equals(first), "Clamp negative time");
        check(WakeUpSequence.samplePhase(1000, false).equals(last), "Clamp completed time");
        System.out.println("Passed: endpoints, fade/rise, exactly two blinks, continuous bounded curves, reduced motion, time clamping");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}



