package dev.shtark.rpganimation;

import net.minecraft.server.level.ServerPlayer;

/** Reset only cosmetic state after an accepted teleport, including same-dimension travel. */
public final class TeleportAnimations {
    private TeleportAnimations() {}
    public static void reset(ServerPlayer player) {
        if (!ServerConfig.TELEPORT_RESET.get()) return;
        IntroServer.finish(player);
        BedAnimationServer.resetAfterTeleport(player);
        ContextAnimationServer.resetAfterTeleport(player);
        BasicActionServer.resetAfterTeleport(player);
        RpgDetailsServer.resetAfterTeleport(player);
        DropAnimationServer.resetAfterTeleport(player);
        PickupAnimationServer.resetAfterTeleport(player);
    }
}
