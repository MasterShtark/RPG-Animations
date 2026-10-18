package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.ObserverPose;
import dev.shtark.rpganimation.client.pose.ModelRoot;
import dev.shtark.rpganimation.client.pose.PoseRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin {
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("HEAD"))
    private void rpgAnimation$reset(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci) {
        ModelRoot.of(this).resetPose();
        ModelRoot.restore(this);
    }
    @Inject(method="setupAttackAnimation",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$attack(LivingEntity entity,float age,CallbackInfo ci) {
        if(entity instanceof AbstractClientPlayer player && dev.shtark.rpganimation.client.BasicAnimations.replaceBodySwing(PoseRegistry.get(player,age-player.tickCount)))ci.cancel();
    }
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void rpgAnimation$pose(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci) {
        if(entity instanceof AbstractClientPlayer player) {
            ModelRoot.capture((HumanoidModel<?>)(Object)this);
            ObserverPose.apply((HumanoidModel<?>)(Object)this,PoseRegistry.get(player,age-player.tickCount));
        }
    }
}
