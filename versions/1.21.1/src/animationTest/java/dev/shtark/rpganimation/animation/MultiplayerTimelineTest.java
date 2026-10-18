package dev.shtark.rpganimation.animation;

import java.util.UUID;

final class MultiplayerTimelineTest {
    static void run() {
        var first = new PlaybackSnapshot(0, 100);
        var late = new PlaybackSnapshot(45, 145);
        for (long tick = 145; tick <= 225; tick++) {
            check(first.time(tick, 0.5) == late.time(tick, 0.5), "Late observer resumes current phase");
        }
        check(late.time(145, 0) == 45, "Late observer never restarts at zero");
        check(late.time(220, 0) == WakeUpSequence.DURATION, "Expires at end");
        check(late.time(10000, 0) == WakeUpSequence.DURATION, "No looping after expiration");
        var cooldown = new PreviewCooldown();
        UUID actor = UUID.randomUUID(), other = UUID.randomUUID();
        check(cooldown.claim(actor, 0), "First request accepted");
        for (int tick = 1; tick < 200; tick++) check(!cooldown.claim(actor, tick), "Spam rejected");
        check(cooldown.claim(other, 10), "Player cooldowns isolated");
        check(cooldown.claim(actor, 200), "Rejected requests do not extend cooldown");
        cooldown.forget(actor);
        check(cooldown.claim(actor, 201), "Logout cleanup");
        cooldown.clear();
        check(cooldown.claim(other, 0), "Server restart cleanup");
        System.out.println("Passed: late-observer phase, expiry, preview rate limit, isolation and cleanup");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

