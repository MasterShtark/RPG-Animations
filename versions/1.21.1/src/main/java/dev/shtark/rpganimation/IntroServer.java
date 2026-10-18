package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.WakeUpSequence;
import dev.shtark.rpganimation.animation.PreviewCooldown;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

@EventBusSubscriber(modid = RpgAnimation.ID)
public final class IntroServer {
    private static final Set<UUID> PENDING = new HashSet<>();
    private static final Map<UUID, Playback> ACTIVE = new HashMap<>();
    private static final PreviewCooldown PREVIEW_COOLDOWN = new PreviewCooldown();
    private record Playback(long startedAt, Vec3 origin) {}

    public static boolean isRunning(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }

    private static long clock(ServerPlayer player) { return player.level().getGameTime(); }

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && player.onGround() && !player.isPassenger()
                && !player.isInWater() && !player.isInLava() && !player.isSpectator()
                && !player.isSleeping() && !BedAnimationServer.isRunning(player) && !player.isFallFlying() && player.hurtTime == 0;
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.getData(RpgAnimation.INTRO_SEEN))
            PENDING.add(player.getUUID());
    }

    /** Requests can only animate their authenticated sender, never choose another player. */
    public static void preview(ServerPlayer player) {
        long now = clock(player);
        UUID id = player.getUUID();
        if (!PREVIEW_COOLDOWN.claim(id, now)) return;
        if (!eligible(player) || ACTIVE.containsKey(id)) return;
        PENDING.remove(id);
        start(player);
    }

    private static void start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), new Playback(clock(player), player.position()));
        PREVIEW_COOLDOWN.reserve(player.getUUID(), clock(player));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new ObserverPayload(player.getUUID(), 0, true));
        PacketDistributor.sendToPlayer(player, new SequencePayload(true));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        Playback run = ACTIVE.get(id);
        if(run!=null && clock(player)-run.startedAt()>=WakeUpSequence.DURATION && eligible(player)
                && run.origin().distanceToSqr(player.position())<=.5) {
            finish(player);
            ContextAnimationServer.begin(player,dev.shtark.rpganimation.animation.ContextClip.BRUSH,player.tickCount%4);
            run=null;
        }
        if (run != null && (clock(player) - run.startedAt() >= WakeUpSequence.DURATION
                || !eligible(player) || run.origin().distanceToSqr(player.position()) > 0.5)) finish(player);
        if (!PENDING.contains(id) || player.tickCount < 40 || !eligible(player)) return;
        PENDING.remove(id);
        player.setData(RpgAnimation.INTRO_SEEN, true);
        start(player);
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finish(player);
        // This is a cosmetic sequence: incoming damage is never cancelled.
    }

    public static void clientFinished(ServerPlayer player) {
        var run=ACTIVE.get(player.getUUID());
        boolean completed=run!=null && clock(player)-run.startedAt()>=WakeUpSequence.DURATION-1
                && eligible(player) && run.origin().distanceToSqr(player.position())<=.5;
        finish(player);
        if(completed)ContextAnimationServer.begin(player,dev.shtark.rpganimation.animation.ContextClip.BRUSH,player.tickCount%4);
    }
    public static void finish(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new ObserverPayload(player.getUUID(), 0, false));
        PacketDistributor.sendToPlayer(player, new SequencePayload(false));
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || !(event.getTarget() instanceof ServerPlayer actor)) return;
        Playback run = ACTIVE.get(actor.getUUID());
        if (run == null) return;
        long age = clock(actor) - run.startedAt();
        if (age >= 0 && age < WakeUpSequence.DURATION)
            PacketDistributor.sendToPlayer(viewer, new ObserverPayload(actor.getUUID(), (int) age, true));
    }

    @SubscribeEvent
    public static void untracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer actor)
            PacketDistributor.sendToPlayer(viewer, new ObserverPayload(actor.getUUID(), 0, false));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finish(player);
        PENDING.remove(event.getEntity().getUUID());
        PREVIEW_COOLDOWN.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PENDING.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) finish(player);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PENDING.clear();
        ACTIVE.clear();
        PREVIEW_COOLDOWN.clear();
    }
}
