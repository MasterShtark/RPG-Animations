package dev.shtark.rpganimation.animation;

/** Original poses for the vanilla player's rigid limbs; model distances are pixels. */
public final class GetUpSequence {
    private GetUpSequence() {}

    public record Frame(double pitch, double roll, double hipHeight,
                        double brace, double headPitch, double eyeFraction) {}

    private static final double[] TIMES = {0, 20, 48, 72, 86, 98, 108, 120};
    private static final Frame[] POSES = {
            new Frame(-Math.PI / 2, 0, 3, 0, .12, .16),
            new Frame(-1.25, 0, 3, 0, .20, .24),
            new Frame(0, 0, 2, 0, 0, .52),
            new Frame(0, 0, 2, 0, 0, .52),
            new Frame(.90, 0, 3, 1, -.45, .56),
            new Frame(1.05, 0, 5, 1, -.65, .65),
            new Frame(.50, 0, 9, 0, -.30, .85),
            new Frame(0, 0, 12, 0, 0, 1)
    };

    private static MotionCurve channel(java.util.function.ToDoubleFunction<Frame> getter) {
        return new MotionCurve(TIMES, java.util.Arrays.stream(POSES).mapToDouble(getter).toArray());
    }
    private static final MotionCurve PITCH = channel(Frame::pitch);
    private static final MotionCurve HEIGHT = channel(Frame::hipHeight);
    private static final MotionCurve BRACE = channel(Frame::brace);
    private static final MotionCurve EYE = channel(Frame::eyeFraction);
    private static final MotionCurve HEAD = new MotionCurve(
            new double[]{0, 24, 50, 72, 89, 101, 111, 120}, .12, .20, 0, 0, -.45, -.65, -.30, 0);

    public static Frame sample(double time) {
        // Keep the hip velocity continuous at the final rise, then stop the legs
        // gently as their ground-contact angle approaches zero.
        double hipTime = time;
        if (time > 108 && time < 120) {
            double u = (time - 108) / 12;
            hipTime = 108 + 12 * (u + u*u - u*u*u);
        }
        return new Frame(PITCH.at(time), 0, HEIGHT.at(hipTime), BRACE.at(time), HEAD.at(time), EYE.at(time));
    }
    /** Both legs draw inward together, retaining the model's normal hip-width pivots. */
    public static double supportLeg(Frame pose) {
        return leadingLeg(pose);
    }

    public static double supportLegRoll(Frame pose) {
        return 0;
    }

    /** Move the other leg from the shared recline into a forward planted stance. */
    public static double leadingLeg(Frame pose) {
        return -Math.acos(Math.max(0, Math.min(1, pose.hipHeight / 12))) - pose.pitch;
    }

    /** Lower the shoulders slightly while bearing weight on the rigid arms. */
    public static double shoulderDrop(Frame pose) {
        return 2 * pose.brace;
    }

    public static double plantedArm(Frame pose) {
        double shoulderHeight = pose.hipHeight + (10 - shoulderDrop(pose)) * Math.cos(pose.pitch);
        // Vanilla arm geometry extends ten pixels below its shoulder pivot.
        double reach = Math.acos(Math.max(0, Math.min(1, shoulderHeight / 10)));
        return (-pose.pitch - reach) * pose.brace;
    }
}



