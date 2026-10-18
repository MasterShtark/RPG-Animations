package dev.shtark.rpganimation.animation;

/** Reach, brace, step up one leg at a time, pivot, then lie down. */
public final class BedEntrySequence {
    public static final int DURATION = 88;
    private static final MotionCurve LEAN = new MotionCurve(
            new double[]{0, 12, 20, 34, 42, 54, 68}, 0, -.35, -.80, -.90, -.95, -.65, 0);
    private static final MotionCurve SUPPORT_ANGLE = new MotionCurve(
            new double[]{0, 20, 28, 40, 54}, -.70, -.70, -.70, -.40, -.10);
    private BedEntrySequence() {}

    public record Frame(double approach, double turn, double recline, double support,
                        double tilt, double supportAngle, double leadLift, double trailLift,
                        double seat, double stride) {
        public double upright() { return 1 - recline; }
    }

    public static Frame sample(double time) {
        double t = Math.max(0, Math.min(DURATION, time));
        double recline = MotionCurve.ease((t - 68) / 20);
        double approach = MotionCurve.ease(t / 20);
        double stride = t < 20 ? .24 * Math.sin(2 * Math.PI * t / 20) * Math.pow(Math.sin(Math.PI * t / 20), 2) : 0;
        return new Frame(approach, MotionCurve.ease(t / 12), recline,
                MotionCurve.ease((t - 6) / 14) * (1 - MotionCurve.ease((t - 54) / 14)),
                LEAN.at(t) + Math.PI * .5 * recline, SUPPORT_ANGLE.at(t),
                MotionCurve.ease((t - 22) / 12), MotionCurve.ease((t - 38) / 16),
                MotionCurve.ease((t - 54) / 14), stride);
    }
}
