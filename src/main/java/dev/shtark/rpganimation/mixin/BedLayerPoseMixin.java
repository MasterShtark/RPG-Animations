package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.client.ObserverPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep attached layers aligned with the same final bed pose as the body. */
@Mixin(LivingEntityRenderer.class)
abstract class BedLayerPoseMixin {
    @Shadow protected EntityModel<?> model;

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"))
    private void rpgAnimation$backpackRoot(net.minecraft.client.renderer.entity.layers.RenderLayer<?,?> layer,
            PoseStack pose,SubmitNodeCollector collector,int light,net.minecraft.client.renderer.entity.state.EntityRenderState state,
            float yaw,float pitch,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        String type=layer.getClass().getName();
        boolean backpack=type.equals("net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackLayerRenderer")
                ||type.equals("com.tiviacz.travelersbackpack.client.renderer.BackpackLayer");
        if(backpack&&state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar
                &&(avatar.getRenderData(dev.shtark.rpganimation.client.ObserverAnimations.TIME)!=null
                    ||avatar.getRenderData(dev.shtark.rpganimation.client.BasicAnimations.FRAME)!=null)
                &&dev.shtark.rpganimation.client.compat.AnimationCompatibility.bodyAllowed(avatar)) {
            pose.pushPose();
            try {
                // These layers attach to body directly and omit the animated parent root.
                model.root().translateAndRotate(pose);
                original.call(layer,pose,collector,light,state,yaw,pitch);
            } finally {pose.popPose();}
        } else original.call(layer,pose,collector,light,state,yaw,pitch);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void rpgAnimation$bedLayers(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                       CameraRenderState camera, CallbackInfo callback) {
        ObserverPose.finishBedPose(model, state);
    }
}
