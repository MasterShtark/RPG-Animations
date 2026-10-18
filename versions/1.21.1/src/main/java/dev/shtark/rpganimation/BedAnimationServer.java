package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.BedEntrySequence;
import dev.shtark.rpganimation.animation.BedExitSequence;
import net.minecraft.core.Direction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Records only successful sleep, without changing vanilla validation or logical movement. */
@EventBusSubscriber(modid = RpgAnimation.ID)
public final class BedAnimationServer {
    private record Run(long start, Vec3 origin, Vec3 bed, float yaw, float pitch, boolean exiting, int facing, boolean reluctant) {
        BedAnimationPayload payload(ServerPlayer player) {
            return new BedAnimationPayload(player.getUUID(), (int)(player.level().getGameTime() - start),
                    true, origin, bed, yaw, pitch, exiting, facing, reluctant);
        }
    }
    private static final Map<UUID, Run> ACTIVE = new HashMap<>();
    public static void begin(ServerPlayer player, Vec3 origin, float yaw, float pitch) {
        if (!player.isSleeping() || player.getBedOrientation() == null || !player.isAlive() || origin.distanceToSqr(player.position()) > 64) return;
        IntroServer.finish(player);
        var run = new Run(player.level().getGameTime(), origin, player.position(), yaw, pitch, false, facing(player.getBedOrientation()), false);
        ACTIVE.put(player.getUUID(), run);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, run.payload(player));
    }
    private static int facing(Direction d) {
        return switch (d) { case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
    }
    public record ExitStart(Vec3 bed, int facing) {}
    public static ExitStart captureExit(ServerPlayer player) {
        var run = ACTIVE.get(player.getUUID());
        if (!player.isSleeping() || player.getBedOrientation() == null || !player.isAlive()
                || player.hurtTime > 0 || (run != null && !run.exiting
                && player.level().getGameTime() - run.start < dev.shtark.rpganimation.animation.AnimationTempo.ENTRY_DURATION)) return null;
        return new ExitStart(player.position(), facing(player.getBedOrientation()));
    }
    public static void exit(ServerPlayer player, ExitStart start) {
        stop(player);
        if (start == null || player.isSleeping() || !player.isAlive() || player.hurtTime > 0
                || player.isPassenger() || start.bed.distanceToSqr(player.position()) > 16) return;
        IntroServer.finish(player);
        var run = new Run(player.level().getGameTime(), player.position(), start.bed,
                player.getYRot(), player.getXRot(), true, start.facing, player.getRandom().nextInt(3)==0);
        ACTIVE.put(player.getUUID(), run);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, run.payload(player));
    }
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var run = ACTIVE.get(player.getUUID());
        if (run != null && (!player.isAlive() || player.isPassenger()
                || player.isSleeping() == run.exiting
                || player.level().getGameTime() - run.start >= (run.exiting ? (run.reluctant ? BedExitSequence.DURATION+16 : BedExitSequence.DURATION) : dev.shtark.rpganimation.animation.AnimationTempo.ENTRY_DURATION)
                || (run.exiting ? run.origin : run.bed).distanceToSqr(player.position()) > (run.exiting ? .04 : .25))) stop(player);
    }
    public static boolean isRunning(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
    private static void stop(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, BedAnimationPayload.stop(player.getUUID()));
    }
    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer actor) {
            var run = ACTIVE.get(actor.getUUID());
            if (run != null && actor.isSleeping() != run.exiting) PacketDistributor.sendToPlayer(viewer, run.payload(actor));
        }
    }
    @SubscribeEvent
    public static void untracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer actor)
            PacketDistributor.sendToPlayer(viewer, BedAnimationPayload.stop(actor.getUUID()));
    }
    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }
    static void resetAfterTeleport(ServerPlayer p) { stop(p); }
}
