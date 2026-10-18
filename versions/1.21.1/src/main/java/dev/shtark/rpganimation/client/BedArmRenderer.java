package dev.shtark.rpganimation.client;

import com.mojang.math.Axis;
import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.BedGeometry;
import dev.shtark.rpganimation.client.pose.PoseKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Dedicated skin arms in world space, using the same bed geometry as observers. */
@EventBusSubscriber(modid=RpgAnimation.ID,value=Dist.CLIENT)
public final class BedArmRenderer {
    public static final PoseKey<Boolean> HIDE_LOCAL_BODY=new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"bed_arms_only"));
    private static PlayerModel<net.minecraft.client.player.AbstractClientPlayer> wide,slim;
    private BedArmRenderer() {}
    public static void reload(EntityRenderersEvent.AddLayers event) {
        wide=new PlayerModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
        slim=new PlayerModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM),true);
    }
    private static boolean active() {
        var mc=Minecraft.getInstance();
        return mc.player!=null&&mc.getCameraEntity()==mc.player&&mc.options.getCameraType().isFirstPerson()
            &&dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed()&&BedAnimations.localUpright(0)>0;
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void vanillaHands(RenderHandEvent event) { if(active())event.setCanceled(true); }
    @SubscribeEvent public static void localBody(RenderPlayerEvent.Pre event) {
        if(event.getEntity()==Minecraft.getInstance().player&&active())event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||!active())return;
        var mc=Minecraft.getInstance(); var p=mc.player;
        if(mc.options.hideGui||p.isInvisible()||p.isSpectator())return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var scene=BedAnimations.localScene(partial);if(scene==null||wide==null||slim==null)return;
        var model=p.getSkin().model()==net.minecraft.client.resources.PlayerSkin.Model.SLIM?slim:wide;
        model.rightArm.resetPose();model.leftArm.resetPose();
        model.rightArm.visible=model.leftArm.visible=true;
        model.rightArm.xRot=(float)scene.arm();model.leftArm.xRot=(float)scene.leftArm();
        model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);
        var camera=event.getCamera().getPosition();var hip=scene.hip();var t=scene.transform();
        float scale=(float)(BedGeometry.MODEL_SCALE*p.getScale());
        int light=mc.getEntityRenderDispatcher().getRenderer(p).getPackedLightCoords(p,partial);
        var stack=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        var type=RenderType.entityTranslucent(p.getSkin().texture());
        stack.pushPose();
        try {
            stack.translate(hip.x()-camera.x,hip.y()-camera.y,hip.z()-camera.z);
            stack.mulPose(Axis.YP.rotationDegrees((float)t.yaw()));
            stack.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(t.tilt())));
            stack.translate(0,-BedGeometry.HIP*scale,0);stack.scale(-scale,-scale,scale);stack.translate(0,-1.501,0);
            var vertices=buffers.getBuffer(type);
            model.rightArm.render(stack,vertices,light,OverlayTexture.NO_OVERLAY);
            model.leftArm.render(stack,vertices,light,OverlayTexture.NO_OVERLAY);
            if(p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE))model.rightSleeve.render(stack,vertices,light,OverlayTexture.NO_OVERLAY);
            if(p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE))model.leftSleeve.render(stack,vertices,light,OverlayTexture.NO_OVERLAY);
        } finally {stack.popPose();}
        buffers.endBatch(type);
    }
}
