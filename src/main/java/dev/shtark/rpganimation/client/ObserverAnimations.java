package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.ObserverPayload;
import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.PlaybackSnapshot;
import dev.shtark.rpganimation.animation.WakeUpSequence;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = RpgAnimation.ID, value = Dist.CLIENT)
public final class ObserverAnimations {
    public static final ContextKey<Double> TIME = new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID, "animation_time"));
    private static final Map<UUID, PlaybackSnapshot> RUNNING = new HashMap<>();
    private static long ticks;
    private static ResourceKey<Level> dimension;

    private ObserverAnimations() {}

    private static void checkDimension() {
        var level = Minecraft.getInstance().level;
        ResourceKey<Level> current = level == null ? null : level.dimension();
        if (!java.util.Objects.equals(current, dimension)) {
            RUNNING.clear();
            dimension = current;
        }
    }

    public static void accept(ObserverPayload payload) {
        checkDimension();
        if (!payload.active() || payload.elapsed() < 0 || payload.elapsed() >= WakeUpSequence.DURATION) {
            RUNNING.remove(payload.player());
            return;
        }
        RUNNING.put(payload.player(), new PlaybackSnapshot(payload.elapsed(), ticks));
    }

    public static Double time(UUID player, double partialTick) {
        var playback = RUNNING.get(player);
        return playback == null ? null : playback.time(ticks, partialTick);
    }

    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                var playback = RUNNING.get(avatar.getUUID());
                Double time = playback == null || ClientConfig.REDUCED_MOTION.get() ? null
                        : playback.time(ticks, state.partialTick);
                state.setRenderData(TIME, time != null && time < WakeUpSequence.DURATION ? time : null);
            }
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        checkDimension();
        if (!Minecraft.getInstance().isPaused()) ticks++;
        RUNNING.values().removeIf(playback -> playback.time(ticks, 0) >= WakeUpSequence.DURATION);
    }

    @SubscribeEvent
    public static void clone(ClientPlayerNetworkEvent.Clone event) {
        RUNNING.remove(event.getOldPlayer().getUUID());
        IntroController.finish(true);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        RUNNING.clear();
        dimension = null;
        ticks = 0;
    }
}
