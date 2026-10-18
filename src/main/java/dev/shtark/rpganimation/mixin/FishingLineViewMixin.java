package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.shtark.rpganimation.client.ContextAnimations;
import dev.shtark.rpganimation.client.compat.AnimationCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.util.context.ContextKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Resolve the native endpoint using this frame's actual hand animation transforms. */
@Mixin(FishingHookRenderer.class)
abstract class FishingLineViewMixin {
    private static final ContextKey<Boolean> LOCAL_ROD = new ContextKey<>(Identifier.fromNamespaceAndPath("rpg_animation", "local_rod"));
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;F)V",at=@At("RETURN"))
    private void rpgAnimation$owner(FishingHook entity,FishingHookRenderState state,float partial,CallbackInfo ci) {
        state.setRenderData(LOCAL_ROD,entity.getPlayerOwner()!=null && entity.getPlayerOwner()==Minecraft.getInstance().player);
    }

    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"))
    private void rpgAnimation$rodAnchor(FishingHookRenderState hook, PoseStack stack,
            SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        var mc=Minecraft.getInstance();
        var owner=mc.player;
        if(owner==null||owner.fishing==null||!Boolean.TRUE.equals(hook.getRenderData(LOCAL_ROD))
                ||!mc.options.getCameraType().isFirstPerson()||mc.getCameraEntity()!=owner
                ||!ContextAnimations.fishingHands()||!AnimationCompatibility.localPresentationAllowed("fishing_hold"))return;
        var player=mc.gameRenderer.gameRenderState().levelRenderState.playerRenderState;
        var hands=player.firstPersonHandsAndItems;
        var avatar=player.avatarRenderState;
        if(hands==null||avatar==null||hands.isScoping)return;
        boolean main=owner.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST);
        var hand=main?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        var arm=main?avatar.mainArm:avatar.mainArm.getOpposite();
        int side=arm==HumanoidArm.RIGHT?1:-1;
        float partial=camera.cameraEntityPartialTicks;
        float height=main?hands.mainHandSwapScale*(1-Mth.lerp(partial,hands.oldMainHandHeight,hands.mainHandHeight))
                :hands.offHandSwapScale*(1-Mth.lerp(partial,hands.oldOffHandHeight,hands.offHandHeight));
        var transforms=(FishingHandTransforms)((FishingHandRendererAccess)mc.gameRenderer).rpgAnimation$hands();
        var rest=new PoseStack();
        transforms.rpgAnimation$equip(rest,arm,0);
        var animated=new PoseStack();
        animated.rotateDegrees(Axis.XP,(hands.viewXRot-hands.xBob)*.1F);
        animated.rotateDegrees(Axis.YP,(hands.viewYRot-hands.yBob)*.1F);
        transforms.rpgAnimation$equip(animated,arm,height);
        var swing=avatar.currentSwing;
        if(swing!=null&&swing.hand()==hand&&(switch(swing.animation().type()){case WHACK -> true;default -> false;}))
            transforms.rpgAnimation$swing(avatar.swingAnimation,animated,side,arm);
        float fov=mc.options.fov().get();
        Vec3 base=mc.getEntityRenderDispatcher().camera.getNearPlane(fov)
                .getPointOnPlane(side*.525F,-.1F).scale(960.0/fov);
        var point=new Vector3f((float)base.x,(float)base.y,(float)base.z);
        camera.viewRotationMatrix.transformDirection(point);
        new Matrix4f(rest.last().pose()).invert().transformPosition(point);
        animated.last().pose().transformPosition(point);
        camera.viewRotationMatrix.invert(new Matrix4f()).transformDirection(point);
        Vec3 origin=owner.getEyePosition(partial).add(point.x,point.y,point.z);
        hook.lineOriginOffset=origin.subtract(owner.fishing.getPosition(partial).add(0,.25,0));
    }
}
