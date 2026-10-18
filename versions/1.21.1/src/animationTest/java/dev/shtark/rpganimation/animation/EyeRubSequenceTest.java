package dev.shtark.rpganimation.animation;

final class EyeRubSequenceTest {
    static void run() {
        var previous = EyeRubSequence.samplePhase(-10, false);
        int peaks = 0;
        double lastDelta = 0;
        for (double t = -9.875; t <= 130; t += 0.125) {
            var frame = EyeRubSequence.samplePhase(t, false);
            check(frame.lift() >= 0 && frame.lift() <= 1, "Bounded arm lift");
            check(frame.horizontal() >= 0 && frame.horizontal() <= 0.045, "Bounded eye rub");
            check(Math.abs(frame.lift() - previous.lift()) < 0.02, "Continuous lift");
            check(Math.abs(frame.horizontal() - previous.horizontal()) < 0.003, "Continuous rubbing");
            double delta = frame.horizontal() - previous.horizontal();
            if (lastDelta > 0 && delta < 0) peaks++;
            if (delta != 0) lastDelta = delta;
            if (t <= 6 || t >= 86) check(frame.lift() == 0 && frame.horizontal() == 0, "Rest outside window");
            var reduced = EyeRubSequence.samplePhase(t, true);
            check(reduced.lift() == 0 && reduced.horizontal() == 0 && reduced.vertical() == 0, "No reduced-motion rub");
            previous = frame;
        }
        for (double closure : new double[] {32, 60}) {
            check(WakeUpSequence.samplePhase(closure, false).eyelids() == 1, "Blink fully closed at rub peak");
            check(EyeRubSequence.samplePhase(closure, false).horizontal() == 0.045, "Rub peaks with blink");
            check(EyeRubSequence.samplePhase(closure, false).lift() == 1, "Hand stays at face through blink");
        }
        check(peaks == 2, "Exactly two rubbing motions");
        check(EyeRubSequence.samplePhase(20, false).lift() == 1, "Raised before rubbing");
        check(EyeRubSequence.samplePhase(72, false).lift() == 1, "Lower only after rubbing");
        check(EyeRubSequence.samplePhase(86, false).lift() == 0, "Clear before intro ends");
        System.out.println("Passed: eye-rub/blink alignment, continuity, two motions, bounds and reduced motion");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
