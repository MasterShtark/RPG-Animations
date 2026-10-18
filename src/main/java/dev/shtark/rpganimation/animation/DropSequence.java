package dev.shtark.rpganimation.animation;

/** A quick release gesture followed by a slower relaxed return. */
public final class DropSequence {
    public static final int DURATION = 10;
    private static final MotionCurve RELEASE = new MotionCurve(new double[]{0, 1.5, 3, 10}, 0, 1, .65, 0);
    private DropSequence() {}
    public static double strength(double time) { return RELEASE.at(Math.max(0, Math.min(DURATION, time))); }
}
