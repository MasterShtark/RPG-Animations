package dev.shtark.rpganimation.animation;

public final class BedExitSequenceTest {
    public static void run() {
        double previous = 88;
        for (double age = 0; age <= 70; age += .1) {
            double phase = BedExitSequence.phase(age);
            check(phase >= 0 && phase <= previous + 1e-8, "Exit phase must progress continuously toward standing");
            check(previous - phase < .3, "No skipped pose at sub-tick sampling");
            previous = phase;
        }
        check(BedExitSequence.phase(-1) == 88 && BedExitSequence.phase(100) == 0, "Bounded endpoints");
        var sitting = BedEntrySequence.sample(BedExitSequence.phase(12));
        check(sitting.recline() == 0 && sitting.seat() == 1, "Sit up before turning to the edge");
        var brace = BedEntrySequence.sample(BedExitSequence.phase(22));
        check(brace.support() == 1 && brace.leadLift() == 1 && brace.trailLift() == 1,
                "Brace both hands before lowering legs");
        var step = BedEntrySequence.sample(BedExitSequence.phase(34));
        check(step.trailLift() == 0 && step.leadLift() == 1 && step.support() == 1,
                "Lower one leg while supported before the other");
        double[][] dirs = {{90,0,1},{0,-1,0},{270,0,-1},{180,1,0}};
        for (var d : dirs) for (var o : new double[][]{{1,0},{-1,0},{0,1},{0,-2}}) {
            var start = scene(d,o,0);
            check(Math.abs(start.transform().tilt() - Math.PI/2) < 1e-8, "Start lying flat");
            var end = scene(d,o,70);
            check(Math.abs(end.transform().x()-o[0]) < 1e-8
                    && Math.abs(end.transform().y()+.6875) < 1e-8
                    && Math.abs(end.transform().z()-o[1]) < 1e-8, "Finish at vanilla-selected exit position");
            check(end.arm() == 0 && end.head() == 0 && Math.abs(end.leadLeg()) < 1e-8
                    && Math.abs(end.trailLeg()) < 1e-8, "Finish with neutral limbs");
            var last = start;
            for(double age=.1;age<=70;age+=.1) {
                var s=scene(d,o,age);
                check(Math.abs(s.hip().x()-last.hip().x()) < .1
                        && Math.abs(s.hip().y()-last.hip().y()) < .1
                        && Math.abs(s.hip().z()-last.hip().z()) < .1, "Continuous body travel");
                last=s;
            }
        }
        var late = new PlaybackSnapshot(54,100);
        check(Math.abs(BedExitSequence.phase(late.time(105,.5))-BedExitSequence.phase(59.5))<1e-8,
                "Late observer uses current exit phase");
        System.out.println("Passed: exit sit/brace/sequential step stages, four-heading endpoints, continuity and late phase");
    }
    private static BedGeometry.Scene scene(double[] d,double[] o,double age) {
        return BedGeometry.scene(o[0],0,o[1],0,.6875,0,75,d[0],(int)d[1],(int)d[2],1.62,1,BedExitSequence.phase(age));
    }
    private static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
