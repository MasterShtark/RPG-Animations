package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.DropSequence;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RpgAnimation.ID)
public final class DropAnimationServer {
    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private static boolean eligible(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isSleeping() && p.hurtTime == 0 && !p.isUsingItem()
                && !BedAnimationServer.isRunning(p) && !IntroServer.isRunning(p);
    }
    public static void dropped(ServerPlayer p) {
        if (!eligible(p)) return;
        long now = p.level().getGameTime();
        Long previous = ACTIVE.get(p.getUUID());
        if (previous != null && now - previous < DropSequence.DURATION) return;
        ACTIVE.put(p.getUUID(), now);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new DropPayload(p.getUUID(), 0, true));
    }
    public static boolean isRunning(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }
    private static void stop(ServerPlayer p) {
        if (ACTIVE.remove(p.getUUID()) != null)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new DropPayload(p.getUUID(), 0, false));
    }
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Long start = ACTIVE.get(p.getUUID());
        if (start != null && (!eligible(p) || p.level().getGameTime()-start >= DropSequence.DURATION)) stop(p);
    }
    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof ServerPlayer viewer && e.getTarget() instanceof ServerPlayer p) {
            Long start = ACTIVE.get(p.getUUID());
            if (start != null && eligible(p)) {
                int age = (int)(p.level().getGameTime()-start);
                if (age >= 0 && age < DropSequence.DURATION)
                    PacketDistributor.sendToPlayer(viewer, new DropPayload(p.getUUID(), age, true));
            }
        }
    }
    @SubscribeEvent
    public static void untracking(PlayerEvent.StopTracking e) {
        if (e.getEntity() instanceof ServerPlayer viewer && e.getTarget() instanceof ServerPlayer p)
            PacketDistributor.sendToPlayer(viewer, new DropPayload(p.getUUID(), 0, false));
    }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) stop(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) stop(p);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) stop(p);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { ACTIVE.clear(); }
    static void resetAfterTeleport(ServerPlayer p) { stop(p); }
}
