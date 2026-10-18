package dev.shtark.rpganimation.animation;

/** Converts elapsed ticks to shared choreography phase without changing contact poses. */
public final class AnimationTempo {
    public static final int WAKE_DURATION = 96;
    public static final int ENTRY_DURATION = 64;
    private static final MotionCurve WAKE = new MotionCurve(
            new double[]{0,8,15,22,29,34,40,47,55,64,71,80,96},
            0,6,20,32,44,48,60,72,86,98,108,115,120);
    private static final MotionCurve ENTRY = new MotionCurve(
            new double[]{0,7,12,20,25,34,43,53,64}, 0,12,20,34,42,54,68,82,88);
    private AnimationTempo() {}
    public static double wake(double ticks) { return WAKE.at(Math.max(0,Math.min(WAKE_DURATION,ticks))); }
    public static double entry(double ticks) { return ENTRY.at(Math.max(0,Math.min(ENTRY_DURATION,ticks))); }
}
