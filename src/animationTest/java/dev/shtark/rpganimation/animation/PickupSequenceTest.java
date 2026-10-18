package dev.shtark.rpganimation.animation;

public final class PickupSequenceTest {
    public static void run() {
        check(PickupSequence.received(64,0) && PickupSequence.received(64,63), "Full and partial transfers accepted");
        check(!PickupSequence.received(64,64) && !PickupSequence.received(0,0)
                && !PickupSequence.received(1,2) && !PickupSequence.received(1,-1), "No transfer/invalid count rejected");
        check(PickupSequence.sample(-1).weight()==0 && PickupSequence.sample(12).weight()==0
                && PickupSequence.sample(100).weight()==0, "Neutral outside playback");
        var last=PickupSequence.sample(0);
        for(double time=.01;time<=12;time+=.01) {
            var f=PickupSequence.sample(time);
            check(Double.isFinite(f.weight()) && f.reach()>=0 && f.reach()<=1
                    && f.gather()>=0 && f.gather()<=1, "Finite bounded motion");
            check(Math.abs(f.reach()-last.reach())<.01 && Math.abs(f.gather()-last.gather())<.01,
                    "Continuous reach and recovery");
            last=f;
        }
        check(PickupSequence.sample(2).reach()==1 && PickupSequence.sample(2).gather()==0,
                "Reach precedes gathering");
        check(PickupSequence.sample(5).gather()==1 && PickupSequence.sample(5).reach()<.8,
                "Gather after reaching");
        check(PickupSequence.sample(.001).weight()/.001<.01
                && PickupSequence.sample(11.999).weight()/.001<.01, "Resting endpoint velocity");
        var late=new PlaybackSnapshot(7,100);
        check(late.time(103,.5)==10.5, "Late tracking resumes current phase");
        System.out.println("Passed: pickup transfer bounds, staged reach/gather, continuity, neutral endpoints and late phase");
    }
    private static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
