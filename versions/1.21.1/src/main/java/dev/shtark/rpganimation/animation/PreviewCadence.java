package dev.shtark.rpganimation.animation;

/** Two captures may be close together, without increasing the sustained rate. */
public final class PreviewCadence {
    private double credits=2;
    private long last=Long.MIN_VALUE;
    public boolean acquire(long now,int interval) {
        if(interval<1)throw new IllegalArgumentException("interval");
        if(last==Long.MIN_VALUE)last=now;
        if(now<last)return false;
        credits=Math.min(2,credits+(double)(now-last)/interval);last=now;
        if(credits<1)return false;
        credits-=1;return true;
    }
    public void reset(){credits=2;last=Long.MIN_VALUE;}
}
