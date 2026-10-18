package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.client.BedAnimations;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class BedRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"), cancellable = true)
    private void rpgAnimation$bed(AvatarRenderState state, PoseStack pose, float yaw, float scale, CallbackInfo callback) {
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.bodyAllowed(state)) return;
        dev.shtark.rpganimation.client.BasicAnimations.mountTransform(state,pose,scale);
        dev.shtark.rpganimation.client.ContextAnimations.seatTransform(state,pose,scale);
        if (BedAnimations.transform(state, pose, scale)) callback.cancel();
    }
}
