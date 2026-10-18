package dev.shtark.rpganimation.animation;

/** Reach down, gather toward the body, then return while walking remains available. */
public final class PickupSequence {
    public static final int DURATION = 12;
    private static final MotionCurve REACH = new MotionCurve(new double[]{0,2,3.5,6,12},0,1,.8,.15,0);
    private static final MotionCurve GATHER = new MotionCurve(new double[]{0,2.5,5,12},0,0,1,0);
    private PickupSequence() {}
    public record Frame(double reach, double gather) {
        public double weight() { return Math.max(reach, gather); }
    }
    public static Frame sample(double time) {
        double t=Math.max(0,Math.min(DURATION,time));
        return new Frame(REACH.at(t), GATHER.at(t));
    }
    public static boolean received(int original, int remaining) {
        return original > 0 && remaining >= 0 && remaining < original;
    }
}
