package dev.shtark.rpganimation.client.compat;

import dev.shtark.rpganimation.client.ClientConfig;
import net.neoforged.fml.ModList;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Method;

/** Optional presentation ownership; no optional types in class signatures. */
final class OptionalPoseCompatibility {
    private static boolean initialized, carryOn, corpse, unavailable;
    private static Method data, carrying;
    private OptionalPoseCompatibility() {}
    private static void initialize() {
        if (initialized) return;
        initialized=true;
        carryOn=ModList.get().isLoaded("carryon");
        corpse=ModList.get().isLoaded("corpse");
        if (!carryOn) return;
        try {
            data=Class.forName("tschipp.carryon.common.carry.CarryOnDataManager").getMethod("getCarryData",Player.class);
            carrying=Class.forName("tschipp.carryon.common.carry.CarryOnData").getMethod("isCarrying");
        } catch (ReflectiveOperationException | LinkageError e) { fail(e); }
    }
    static boolean owns(Object player) {
        if (player==null) return false;
        initialize();
        if (corpse && ClientConfig.CORPSE_HANDOFF.get()) {
            for (Class<?> type=player.getClass();type!=null;type=type.getSuperclass())
                if (type.getName().equals("de.maxhenkel.corpse.entities.DummyPlayer")) return true;
        }
        if (!carryOn || unavailable || !ClientConfig.CARRY_ON_HANDOFF.get() || !(player instanceof Player)) return false;
        try {
            Object state=data.invoke(null,player);
            return state!=null && Boolean.TRUE.equals(carrying.invoke(state));
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) { fail(e);return false; }
    }
    private static void fail(Throwable error) {
        if (!unavailable) com.mojang.logging.LogUtils.getLogger().warn("RPG Animation could not read Carry On status; its pose handoff is disabled. Check the installed Carry On version.",error);
        unavailable=true;
    }
}
