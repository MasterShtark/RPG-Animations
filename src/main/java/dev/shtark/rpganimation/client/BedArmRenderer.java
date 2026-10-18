package dev.shtark.rpganimation.client;

import com.mojang.math.Axis;
import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.BedGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** World-space skin arms; vanilla's first-person hand pass is disabled while asleep. */
@EventBusSubscriber(modid = RpgAnimation.ID, value = Dist.CLIENT)
public final class BedArmRenderer {
    public static final ContextKey<Boolean> HIDE_LOCAL_BODY = new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID, "bed_arms_only"));
    private static final ContextKey<Hands> HANDS = new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID, "bed_hands"));
    private record Hands(BedGeometry.Scene pose, Identifier texture, boolean slim, boolean rightSleeve,
                         boolean leftSleeve, float scale, int light) {}
    private static PlayerModel wide, slim;
    private BedArmRenderer() {}

    public static void reload(EntityRenderersEvent.AddLayers event) {
        wide = new PlayerModel(event.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        var state = event.getRenderState();
        state.setRenderData(HANDS, null);
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed()) return;
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.getCameraEntity() != p || !event.getOptionsRenderState().cameraType.isFirstPerson()
                || p.isInvisible() || p.isSpectator()) return;
        var pose = BedAnimations.localScene(event.getCamera().getCameraEntityPartialTicks(event.getDeltaTracker()));
        if (pose == null || wide == null || slim == null) return;
        // Vanilla also submits its sleeping camera entity. Replace that local body
        // with just these arms so its head/torso cannot cover the view or draw twice.
        for (var entity : state.entityRenderStates) {
            if (entity instanceof AvatarRenderState avatar && avatar.id == p.getId())
                avatar.setRenderData(HIDE_LOCAL_BODY, true);
        }
        if (state.playerRenderState.avatarRenderState != null)
            state.playerRenderState.avatarRenderState.setRenderData(HIDE_LOCAL_BODY, true);
        if (mc.gui.hud.isHidden()) return;
        var skin = p.getSkin();
        state.setRenderData(HANDS, new Hands(pose, skin.body().texturePath(), skin.model() == PlayerModelType.SLIM,
                p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),
                p.getScale(), mc.getEntityRenderDispatcher().getRenderer(p).getPackedLightCoords(p, event.getCamera().getCameraEntityPartialTicks(event.getDeltaTracker()))));
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void vanillaHands(RenderHandEvent event) {
        if (dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed() && BedAnimations.localUpright(0) > 0) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void localBody(RenderPlayerEvent.Pre<?> event) {
        if (Boolean.TRUE.equals(event.getRenderState().getRenderData(HIDE_LOCAL_BODY))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void submit(SubmitCustomGeometryEvent event) {
        var hands = event.getLevelRenderState().getRenderData(HANDS);
        if (hands == null) return;
        PlayerModel model = hands.slim ? slim : wide;
        if (model == null) return;
        // Dedicated baked arms avoid changing shared avatar models or rerunning NEA.
        model.rightArm.resetPose(); model.leftArm.resetPose();
        model.rightArm.visible = model.leftArm.visible = true;
        model.rightArm.xRot = (float) hands.pose.arm();
        model.leftArm.xRot = (float) hands.pose.leftArm();
        model.rightSleeve.visible = hands.rightSleeve;
        model.leftSleeve.visible = hands.leftSleeve;
        var t = hands.pose.transform();
        var hip = hands.pose.hip();
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        double modelScale = BedGeometry.MODEL_SCALE * hands.scale;
        var stack = event.getPoseStack();
        stack.pushPose();
        try {
            stack.translate(hip.x() - camera.x, hip.y() - camera.y, hip.z() - camera.z);
            stack.rotateDegrees(Axis.YP, (float)t.yaw());
            stack.rotateDegrees(Axis.XP, (float)Math.toDegrees(t.tilt()));
            stack.translate(0, -BedGeometry.HIP * modelScale, 0);
            stack.scale((float)-modelScale, (float)-modelScale, (float)modelScale);
            stack.translate(0, -1.501, 0);
            var type = RenderTypes.entityTranslucent(hands.texture);
            var collector = event.getSubmitNodeCollector();
            collector.submitModelPart(model.rightArm, stack, type, hands.light, OverlayTexture.NO_OVERLAY, null);
            collector.submitModelPart(model.leftArm, stack, type, hands.light, OverlayTexture.NO_OVERLAY, null);
        } finally {
            stack.popPose();
        }
    }
}
