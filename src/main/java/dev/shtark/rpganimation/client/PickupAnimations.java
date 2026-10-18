package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.PickupPayload;
import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.PlaybackSnapshot;
import dev.shtark.rpganimation.animation.PickupSequence;
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
public final class PickupAnimations {
    public static final ContextKey<Double> TIME = new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID, "pickup_time"));
    private static final Map<UUID, PlaybackSnapshot> RUNNING = new HashMap<>();
    private static long ticks;
    private static ResourceKey<Level> dimension;

    private PickupAnimations() {}

    private static void checkDimension() {
        var level = Minecraft.getInstance().level;
        ResourceKey<Level> current = level == null ? null : level.dimension();
        if (!java.util.Objects.equals(current, dimension)) {
            RUNNING.clear();
            dimension = current;
        }
    }

    public static void accept(PickupPayload payload) {
        checkDimension();
        if (!payload.active() || payload.elapsed() < 0 || payload.elapsed() >= PickupSequence.DURATION) {
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
                || !ClientConfig.PICKUP_ANIMATION.get() || ClientConfig.REDUCED_MOTION.get()
                || DropAnimations.localActive() || IntroController.active() || BedAnimations.isRunning(p.getUUID())) return false;
        Double age = time(p.getUUID(), 0);
        return age != null && age < PickupSequence.DURATION;
    }

    public static void firstPerson(AvatarRenderState avatar, net.minecraft.world.InteractionHand hand,
                                    com.mojang.blaze3d.vertex.PoseStack pose) {
        if (avatar == null || hand != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        Double time = avatar.getRenderData(TIME);
        if (time == null) return;
        var motion = PickupSequence.sample(time);
        float side = avatar.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(-side * .10 * motion.gather(), -.14 * motion.reach() + .22 * motion.gather(),
                -.20 * motion.reach() + .10 * motion.gather());
        pose.rotateDegrees(com.mojang.math.Axis.XP, (float)(25 * motion.reach() - 18 * motion.gather()));
        pose.rotateDegrees(com.mojang.math.Axis.ZP, (float)(side * 10 * motion.gather()));
    }

    public static void model(net.minecraft.client.model.HumanoidModel<?> model, AvatarRenderState avatar) {
        Double time = avatar.getRenderData(TIME);
        if (time == null) return;
        var motion = PickupSequence.sample(time);
        float amount = (float)motion.weight();
        boolean right = avatar.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT;
        var arm = right ? model.rightArm : model.leftArm;
        arm.xRot = arm.xRot * (1 - amount) - (float)(.45 * motion.reach() + 1.25 * motion.gather());
        arm.yRot *= 1 - amount;
        arm.zRot += ((right ? -.12f : .12f) - arm.zRot) * amount;
    }

    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                var playback = RUNNING.get(avatar.getUUID());
                Double time = playback == null || !ClientConfig.PICKUP_ANIMATION.get() || ClientConfig.REDUCED_MOTION.get()
                        || DropAnimations.time(avatar.getUUID(), state.partialTick) != null || BedAnimations.isRunning(avatar.getUUID()) || ObserverAnimations.time(avatar.getUUID(), state.partialTick) != null
                        || state.hasPose(net.minecraft.world.entity.Pose.SLEEPING) || state.deathTime > 0 ? null
                        : playback.time(ticks, state.partialTick);
                state.setRenderData(TIME, time != null && time < PickupSequence.DURATION ? time : null);
            }
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        checkDimension();
        if (!Minecraft.getInstance().isPaused()) ticks++;
        RUNNING.values().removeIf(playback -> playback.time(ticks, 0) >= PickupSequence.DURATION);
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
