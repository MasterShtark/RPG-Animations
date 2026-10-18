package dev.shtark.rpganimation.animation;

public final class GetUpSequenceTest {
    public static void run() {
        var end = GetUpSequence.sample(120);
        check(end.pitch() == 0 && end.roll() == 0 && end.hipHeight() == 12
                && end.brace() == 0 && end.headPitch() == 0, "Neutral standing endpoint");
        check(GetUpSequence.sample(-10).equals(GetUpSequence.sample(0)), "Clamp start");
        check(GetUpSequence.sample(140).equals(end), "Clamp end");
        var crouch = GetUpSequence.sample(90);
        check(crouch.pitch() > .4 && crouch.hipHeight() < 8, "Lean forward before pushing up");
        for (double t = 48; t <= 72; t += .25) {
            var seated = GetUpSequence.sample(t);
            check(seated.pitch() == 0 && seated.hipHeight() == 2,
                    "Upright seated hold with both legs forward");
        }
        for (double t = 86; t <= 98; t += .1) {
            var p = GetUpSequence.sample(t);
            check(p.brace() == 1 && EyeRubSequence.sample(t, false).lift() == 0,
                    "Both hands available for ground support after eye rub");
            double shoulderHeight = p.hipHeight() + (10 - GetUpSequence.shoulderDrop(p)) * Math.cos(p.pitch());
            check(Math.abs(shoulderHeight - 10 * Math.cos(p.pitch() + GetUpSequence.plantedArm(p))) < 1e-8,
                    "Hand endpoint reaches ground during push");
        }
        var previous = GetUpSequence.sample(0);
        for (double t = .05; t < 120; t += .05) {
            var p = GetUpSequence.sample(t);
            check(Double.isFinite(p.pitch()) && p.hipHeight() >= 2 && p.hipHeight() <= 12,
                    "Finite bounded pose");
            check(Math.abs(p.pitch() - previous.pitch()) < .01
                    && Math.abs(p.hipHeight() - previous.hipHeight()) < .04, "Continuous body motion");
            double leg = GetUpSequence.supportLeg(p), roll = GetUpSequence.supportLegRoll(p);
            check(Math.abs(leg - GetUpSequence.leadingLeg(p)) < 1e-9 && roll == 0,
                    "No split or sideways leg sweep during the rise");
            double footDown = 12 * (Math.cos(p.pitch()) * Math.cos(roll) * Math.cos(leg)
                    - Math.sin(p.pitch()) * Math.sin(leg));
            check(Math.abs(footDown - p.hipHeight()) < 1e-7, "Support foot stays on unrolled ground plane");
            previous = p;
        }
        // At the planted crouch the unrolled rigid feet meet the ground plane.
        for (double leg : new double[]{GetUpSequence.leadingLeg(crouch), GetUpSequence.supportLeg(crouch)}) {
            check(Math.abs(12 * Math.cos(crouch.pitch() + leg) - crouch.hipHeight()) < 1e-8,
                    "Planted crouch foot height");
        }
        System.out.println("Passed: seated hold, hand support, compact leg stance, ground reach, continuity, neutral finish");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

