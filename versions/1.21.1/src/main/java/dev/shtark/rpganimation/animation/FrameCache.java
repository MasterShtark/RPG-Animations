package dev.shtark.rpganimation.animation;

import java.util.IdentityHashMap;
import java.util.function.Function;

/** Render-thread-only snapshots; never retains player references between frames. */
public final class FrameCache<K,V> {
    private final IdentityHashMap<K,V> values = new IdentityHashMap<>();
    private boolean rendering;
    public void begin() { values.clear(); rendering = true; }
    public void end() { rendering = false; values.clear(); }
    public V get(K key, Function<K,V> read) {
        if (!rendering) return read.apply(key);
        if (values.containsKey(key)) return values.get(key);
        V value = read.apply(key);
        values.put(key, value);
        return value;
    }
    public int retainedEntries() { return values.size(); }
}
