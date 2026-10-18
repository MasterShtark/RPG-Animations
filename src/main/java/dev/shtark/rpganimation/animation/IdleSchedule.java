package dev.shtark.rpganimation.animation;

/** Avatar inactivity, not an AFK detector. No queued gestures survive activity. */
public final class IdleSchedule {
    private final java.util.Random random;
    private int ticks;
    private int small,medium,large;
    public IdleSchedule(long seed){random=new java.util.Random(seed);schedule();}
    private void schedule(){small=480+random.nextInt(361);medium=small+480+random.nextInt(361);large=medium+960+random.nextInt(721);}
    public ContextClip tick(boolean quiet) {
        if(!quiet){if(ticks>0)schedule();ticks=0;return null;}
        ticks++;
        if(ticks==small)return ContextClip.STRETCH_SMALL;
        if(ticks==medium)return ContextClip.STRETCH_MEDIUM;
        if(ticks==large){ticks=0;schedule();return ContextClip.STRETCH_LARGE;}
        return null;
    }
}
