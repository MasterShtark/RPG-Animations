package dev.shtark.rpganimation.animation;

/** Resumes a server snapshot at its elapsed tick rather than restarting late observers. */
public record PlaybackSnapshot(int elapsed, long receivedAt) {
    public double time(long clientTick, double partialTick) {
        return Math.max(0, Math.min(WakeUpSequence.DURATION,
                elapsed + Math.max(0, clientTick - receivedAt) + partialTick));
    }
}
