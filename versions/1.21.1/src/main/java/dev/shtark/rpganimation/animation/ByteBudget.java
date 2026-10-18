package dev.shtark.rpganimation.animation;

/** Fixed-window byte ceiling; caller supplies one monotonic clock and window unit. */
public final class ByteBudget {
    private long window=Long.MIN_VALUE,used;
    public synchronized boolean take(long currentWindow,int bytes,int limit) {
        if(bytes<0||limit<0)return false;
        // Concurrent decoders can arrive late with a timestamp captured before another thread.
        if(currentWindow<window)return false;
        if(window!=currentWindow){window=currentWindow;used=0;}
        if(bytes>limit-used)return false;
        used+=bytes;return true;
    }
    public synchronized void reset(){window=Long.MIN_VALUE;used=0;}
}
