package dev.shtark.rpganimation.animation;

/** Arbitration shared by automatic and explicit self-preview requests. */
public final class ContextPolicy {
    private ContextPolicy() {}
    public static boolean mayStart(boolean eligible,long now,long nextAllowed,ContextClip active,ContextClip requested) {
        return eligible && now>=nextAllowed && (active==null || requested.priority>active.priority);
    }
}
