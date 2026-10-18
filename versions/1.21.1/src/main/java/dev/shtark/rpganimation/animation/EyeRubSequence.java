package dev.shtark.rpganimation.animation;

/** Local first-person arm motion, measured in ticks and render-space blocks. */
public final class EyeRubSequence {
    private EyeRubSequence() {}

    public record Frame(double lift, double horizontal, double vertical) {}

    public static Frame sample(double ticks, boolean reducedMotion) {
        return samplePhase(AnimationTempo.wake(ticks), reducedMotion);
    }

    public static Frame samplePhase(double ticks, boolean reducedMotion) {
        if (reducedMotion || ticks <= 6 || ticks >= 86) return new Frame(0, 0, 0);
        double lift = smooth((ticks - 6) / 14) * (1 - smooth((ticks - 72) / 14));
        // Each inward stroke peaks at the corresponding full eyelid closure.
        double rub = pulse(ticks, 20, 44) + pulse(ticks, 48, 72);
        return new Frame(lift, 0.045 * rub, 0.025 * rub);
    }

    private static double pulse(double ticks, double start, double end) {
        if (ticks <= start || ticks >= end) return 0;
        double phase = (ticks - start) / (end - start) * 2 * Math.PI;
        return (1 - Math.cos(phase)) * 0.5;
    }

    private static double smooth(double value) {
        double x = Math.max(0, Math.min(1, value));
        return x * x * (3 - 2 * x);
    }
}
