package dev.shtark.rpganimation.animation;

public final class BedEntrySequenceTest {
    public static void run() {
        check(BedEntrySequence.sample(0).upright() == 1, "Starts upright at approach");
        check(BedEntrySequence.sample(6).upright() == 1, "Upright while approaching");
        var end = BedEntrySequence.sample(BedEntrySequence.DURATION);
        check(end.upright() == 0 && end.support() == 0, "Vanilla sleeping endpoint");
        check(BedEntrySequence.sample(-10).equals(BedEntrySequence.sample(0)), "Clamp start");
        check(BedEntrySequence.sample(100).equals(end), "Clamp end");
        var previous = BedEntrySequence.sample(0);
        for (double t = .1; t < BedEntrySequence.DURATION + 2; t += .1) {
            var frame = BedEntrySequence.sample(t);
            check(frame.upright() >= 0 && frame.upright() <= previous.upright(), "Monotonic recline");
            check(frame.support() >= 0 && frame.support() <= 1, "Bounded hand support");
            check(Math.abs(frame.upright() - previous.upright()) < .02
                    && Math.abs(frame.support() - previous.support()) < .03, "Continuous poses");
            previous = frame;
        }
        BedGeometryTest.run();
        BedClimbTest.run();
        var snapshot = new PlaybackSnapshot(20, 100);
        check(BedEntrySequence.sample(snapshot.time(105, 0)).equals(BedEntrySequence.sample(25)),
                "Late observer resumes at elapsed phase");
        System.out.println("Passed: bounded bed climb/recline timing, vanilla endpoint and late phase");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}

