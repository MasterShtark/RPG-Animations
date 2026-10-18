package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.ObserverPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin {
    @Inject(method="setupAttackAnimation",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$attack(HumanoidRenderState state,CallbackInfo callback) {
        if(state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar
                &&dev.shtark.rpganimation.client.BasicAnimations.replaceBodySwing(avatar))callback.cancel();
    }
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void rpgAnimation$pose(HumanoidRenderState state, CallbackInfo callback) {
        ObserverPose.apply((HumanoidModel<?>) (Object) this, state);
    }
}
