package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.client.ObserverPose;
import dev.shtark.rpganimation.client.BedAnimations;
import dev.shtark.rpganimation.client.pose.ModelRoot;
import dev.shtark.rpganimation.client.pose.PoseRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Apply a common parent transform to the body and every attached layer. */
@Mixin(LivingEntityRenderer.class)
abstract class BedLayerPoseMixin {
    @Shadow protected EntityModel<?> model;
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("RETURN"))
    private void rpgAnimation$restore(LivingEntity entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffer,int light,CallbackInfo ci) {
        ModelRoot.restore(model);
    }
    @WrapOperation(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hasPose(Lnet/minecraft/world/entity/Pose;)Z"))
    private boolean rpgAnimation$visualSleep(LivingEntity entity,Pose pose,Operation<Boolean> original,
            LivingEntity rendered,float yaw,float partial,PoseStack stack,MultiBufferSource buffer,int light) {
        if(entity instanceof AbstractClientPlayer player && pose==Pose.SLEEPING && PoseRegistry.get(player,partial).getRenderData(BedAnimations.POSE)!=null)return false;
        return original.call(entity,pose);
    }
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void rpgAnimation$allLayers(LivingEntity entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffer,int light,CallbackInfo ci) {
        if(!(entity instanceof AbstractClientPlayer player))return;
        ObserverPose.finishBedPose(model,PoseRegistry.get(player,partial));
        if(model instanceof PlayerModel<?> m) {
            m.hat.copyFrom(m.head); m.jacket.copyFrom(m.body);
            m.rightSleeve.copyFrom(m.rightArm); m.leftSleeve.copyFrom(m.leftArm);
            m.rightPants.copyFrom(m.rightLeg); m.leftPants.copyFrom(m.leftLeg);
        }
        ModelRoot.of(model).translateAndRotate(stack);
    }
}
