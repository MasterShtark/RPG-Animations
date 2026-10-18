package dev.shtark.rpganimation.animation;

/** Retimes the contact-preserving rig for sitting up, stepping down and pushing upright. */
public final class BedExitSequence {
    public static final int DURATION = 70;
    private static final MotionCurve PHASE = new MotionCurve(
            new double[]{0, 12, 22, 34, 48, 70}, 88, 68, 54, 38, 20, 0);
    private BedExitSequence() {}
    private static final MotionCurve RELUCTANT = new MotionCurve(
            new double[]{0,12,28,38,50,64,86},88,68,68,54,38,20,0);
    public static double reluctantPhase(double time) {
        return RELUCTANT.at(time);
    }
    public static double faceRub(double time) {
        return MotionCurve.ease((time-12)/4)*(1-MotionCurve.ease((time-23)/5));
    }
    public static double phase(double time) { return PHASE.at(Math.max(0, Math.min(DURATION, time))); }
}
