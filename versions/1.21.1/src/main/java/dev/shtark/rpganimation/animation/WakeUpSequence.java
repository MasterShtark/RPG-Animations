package dev.shtark.rpganimation.animation;

/** Pure timeline shared by rendering and regression checks. Times are in game ticks. */
public final class WakeUpSequence {
    public static final int DURATION = AnimationTempo.WAKE_DURATION;
    private WakeUpSequence() {}

    public record Frame(double eyeFraction, float pitchOffset, float roll, float darkness, float eyelids) {}

    public static Frame sample(double ticks, boolean reducedMotion) {
        return samplePhase(AnimationTempo.wake(ticks), reducedMotion);
    }

    public static Frame samplePhase(double ticks, boolean reducedMotion) {
        double time = Math.max(0, Math.min(120, ticks));
        var pose = GetUpSequence.sample(time);
        double rise = (pose.eyeFraction() - .16) / .84;
        float darkness = (float) (1 - smooth((time - 6) / 60));
        float eyelids = reducedMotion ? 0 : (float) (1 - opening(time));
        return new Frame(reducedMotion ? 1 : 0.16 + 0.84 * rise,
                reducedMotion ? 0 : (float) (22 * (1 - rise)),
                reducedMotion ? 0 : (float) (Math.toDegrees(pose.roll()) - 7 * (1 - rise)), darkness, eyelids);
    }

    /** Normal hands rise from below the viewport as the shared get-up settles. */
    public static double handLowering(double ticks) {
        return 1 - smooth((ticks - (DURATION - 12)) / 12);
    }

    private static double opening(double time) {
        if (time < 20) return 0.65 * smooth((time - 6) / 14);
        if (time < 32) return 0.65 * (1 - smooth((time - 20) / 12));
        if (time < 48) return 0.85 * smooth((time - 32) / 16);
        if (time < 60) return 0.85 * (1 - smooth((time - 48) / 12));
        return smooth((time - 60) / 22);
    }

    /** Fraction of screen height covered by each lid at horizontal position [-1, 1]. */
    public static double lidInset(double horizontal, double closure) {
        double closed = Math.max(0, Math.min(1, closure));
        double x = Math.max(-1, Math.min(1, horizontal));
        return Math.min(0.5, 0.5 * Math.pow(closed, 1.6) + 0.10 * Math.sin(Math.PI * closed) * x * x);
    }

    public static int blockyLidHeight(double horizontal, double closure, int height, int step) {
        if (closure <= 0) return 0;
        int maximum = (height + 1) / 2;
        if (closure >= 1) return maximum;
        return Math.min(maximum, (int) Math.round(height * lidInset(horizontal, closure) / step) * step);
    }

    private static double smooth(double value) {
        double x = Math.max(0, Math.min(1, value));
        return x * x * (3 - 2 * x);
    }
}
