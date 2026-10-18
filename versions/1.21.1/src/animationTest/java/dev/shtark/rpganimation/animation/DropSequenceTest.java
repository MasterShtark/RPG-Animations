package dev.shtark.rpganimation.animation;

public final class DropSequenceTest {
    public static void run() {
        check(DropSequence.strength(-1)==0 && DropSequence.strength(0)==0
                && DropSequence.strength(10)==0 && DropSequence.strength(100)==0, "Neutral at and outside endpoints");
        double last=0; int peaks=0; boolean rising=true;
        for(double time=.01;time<=10;time+=.01) {
            double amount=DropSequence.strength(time);
            check(Double.isFinite(amount) && amount>=-1e-8 && amount<=1+1e-8, "Bounded finite motion");
            check(Math.abs(amount-last)<.02, "No abrupt change at sub-tick boundaries");
            if(rising && amount<last) { peaks++; rising=false; }
            if(!rising) check(amount<=last+1e-8, "Recovery never rethrows");
            last=amount;
        }
        check(peaks==1 && DropSequence.strength(1.5)>.99 && DropSequence.strength(10)<.5,
                "One quick release followed by relaxed recovery");
        double epsilon=.001;
        check(DropSequence.strength(epsilon)/epsilon<.01
                && DropSequence.strength(10-epsilon)/epsilon<.01, "Resting endpoint velocities");
        var snapshot=new PlaybackSnapshot(5,100);
        check(Math.abs(snapshot.time(103,.5)-8.5)<1e-8, "Late observer retains phase");
        System.out.println("Passed: drop single release, bounded continuity, neutral endpoints/velocity and late phase");
    }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
