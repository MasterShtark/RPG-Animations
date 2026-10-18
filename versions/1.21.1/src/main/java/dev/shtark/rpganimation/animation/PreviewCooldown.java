package dev.shtark.rpganimation.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player server cooldown; rejected requests cannot extend another player's timer. */
public final class PreviewCooldown {
    private final Map<UUID, Long> next = new HashMap<>();

    public boolean claim(UUID player, long now) {
        if (now < next.getOrDefault(player, Long.MIN_VALUE)) return false;
        reserve(player, now);
        return true;
    }

    public void reserve(UUID player, long now) { next.put(player, now + 200); }
    public void forget(UUID player) { next.remove(player); }
    public void clear() { next.clear(); }
}
