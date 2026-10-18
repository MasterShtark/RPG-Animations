package dev.shtark.rpganimation.animation;

/** Shape-preserving cubic interpolation with continuous velocity between keys. */
public final class MotionCurve {
    private final double[] times, values, slopes;
    public MotionCurve(double[] times, double... values) {
        if (times.length != values.length || times.length < 2) throw new IllegalArgumentException("Key count");
        this.times = times.clone(); this.values = values.clone(); slopes = new double[times.length];
        for (int i = 1; i < times.length; i++)
            if (times[i] <= times[i - 1]) throw new IllegalArgumentException("Key order");
        for (int i = 1; i < times.length - 1; i++) {
            double left = times[i] - times[i - 1], right = times[i + 1] - times[i];
            double a = (values[i] - values[i - 1]) / left, b = (values[i + 1] - values[i]) / right;
            if (a * b > 0) {
                double w1 = 2 * right + left, w2 = right + 2 * left;
                slopes[i] = (w1 + w2) / (w1 / a + w2 / b);
            }
        }
    }
    public double at(double time) {
        if (time <= times[0]) return values[0];
        for (int i = 1; i < times.length; i++) {
            if (time <= times[i]) {
                if (values[i] == values[i - 1]) return values[i];
                double span = times[i] - times[i - 1], x = (time - times[i - 1]) / span;
                double x2 = x * x, x3 = x2 * x;
                return (2*x3-3*x2+1)*values[i-1] + (x3-2*x2+x)*span*slopes[i-1]
                        + (-2*x3+3*x2)*values[i] + (x3-x2)*span*slopes[i];
            }
        }
        return values[values.length - 1];
    }
    public static double ease(double value) {
        double x = Math.max(0, Math.min(1, value));
        return x*x*x*(x*(x*6-15)+10);
    }
}

