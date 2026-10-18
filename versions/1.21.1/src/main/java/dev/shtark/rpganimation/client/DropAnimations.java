package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.DropPayload;
import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.PlaybackSnapshot;
import dev.shtark.rpganimation.animation.DropSequence;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;

import dev.shtark.rpganimation.client.pose.PlayerFrame;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import dev.shtark.rpganimation.client.pose.PoseKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import dev.shtark.rpganimation.client.pose.PoseSampler;
import dev.shtark.rpganimation.client.pose.PoseRegistry;

@EventBusSubscriber(modid = RpgAnimation.ID, value = Dist.CLIENT)
public final class DropAnimations {
    public static final PoseKey<Double> TIME = new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "drop_time"));
    private static final Map<UUID, PlaybackSnapshot> RUNNING = new HashMap<>();
    private static long ticks;
    private static ResourceKey<Level> dimension;

    private DropAnimations() {}

    private static void checkDimension() {
        var level = Minecraft.getInstance().level;
        ResourceKey<Level> current = level == null ? null : level.dimension();
        if (!java.util.Objects.equals(current, dimension)) {
            RUNNING.clear();
            dimension = current;
        }
    }

    public static void accept(DropPayload payload) {
        checkDimension();
        if (!payload.active() || payload.elapsed() < 0 || payload.elapsed() >= DropSequence.DURATION) {
            RUNNING.remove(payload.player());
            return;
        }
        RUNNING.put(payload.player(), new PlaybackSnapshot(payload.elapsed(), ticks));
    }

    public static Double time(UUID player, double partialTick) {
        var playback = RUNNING.get(player);
        return playback == null ? null : playback.time(ticks, partialTick);
    }

    public static boolean localActive() {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.isAlive() || p.isSleeping() || p.isUsingItem() || p.hurtTime > 0
                || !ClientConfig.DROP_ANIMATION.get() || ClientConfig.REDUCED_MOTION.get()
                || IntroController.active() || BedAnimations.isRunning(p.getUUID())) return false;
        Double age = time(p.getUUID(), 0);
        return age != null && age < DropSequence.DURATION;
    }

    public static void firstPerson(PlayerFrame avatar, net.minecraft.world.InteractionHand hand,
                                    com.mojang.blaze3d.vertex.PoseStack pose) {
        if (avatar == null || hand != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        Double time = avatar.getRenderData(TIME);
        if (time == null) return;
        double amount = DropSequence.strength(time);
        float side = avatar.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(-side * .06 * amount, -.10 * amount, -.24 * amount);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float)(16 * amount)));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float)(-side * 7 * amount)));
    }

    public static void model(net.minecraft.client.model.HumanoidModel<?> model, PlayerFrame avatar) {
        Double time = avatar.getRenderData(TIME);
        if (time == null) return;
        float amount = (float)DropSequence.strength(time);
        boolean right = avatar.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT;
        var arm = right ? model.rightArm : model.leftArm;
        arm.xRot += (-.95f - arm.xRot) * amount;
        arm.yRot *= 1 - amount;
        arm.zRot += ((right ? -.12f : .12f) - arm.zRot) * amount;
    }

    public static void register(PoseRegistry event) {
        event.registerAvatarEntityModifier(new PoseSampler() {
            @Override
            public void accept(net.minecraft.world.entity.LivingEntity avatar, PlayerFrame state) {
                var playback = RUNNING.get(avatar.getUUID());
                Double time = playback == null || !ClientConfig.DROP_ANIMATION.get() || ClientConfig.REDUCED_MOTION.get()
                        || BedAnimations.isRunning(avatar.getUUID()) || ObserverAnimations.time(avatar.getUUID(), state.partialTick) != null
                        || state.hasPose(net.minecraft.world.entity.Pose.SLEEPING) || state.deathTime > 0 ? null
                        : playback.time(ticks, state.partialTick);
                state.setRenderData(TIME, time != null && time < DropSequence.DURATION ? time : null);
            }
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        checkDimension();
        if (!Minecraft.getInstance().isPaused()) ticks++;
        RUNNING.values().removeIf(playback -> playback.time(ticks, 0) >= DropSequence.DURATION);
    }

    @SubscribeEvent
    public static void clone(ClientPlayerNetworkEvent.Clone event) {
        RUNNING.remove(event.getOldPlayer().getUUID());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        RUNNING.clear();
        dimension = null;
        ticks = 0;
    }
}
