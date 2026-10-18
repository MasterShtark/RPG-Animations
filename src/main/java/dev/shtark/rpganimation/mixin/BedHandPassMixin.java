package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.BedAnimations;
import dev.shtark.rpganimation.client.compat.AnimationCompatibility;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bed arms are submitted in world space; skip the competing item-space pass. */
@Mixin(value=GameRenderer.class, priority=2000)
abstract class BedHandPassMixin {
    @Inject(method="renderItemInHand",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$bedHandsOnly(CallbackInfo ci) {
        if(BedAnimations.localUpright(0)>0&&AnimationCompatibility.localPresentationAllowed())ci.cancel();
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method="renderItemInHand",
        at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"))
    private void rpgAnimation$wakeHandsOnly(
            net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer renderer,
            float partial, com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.PlayerRenderState player,
            net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState hands,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!dev.shtark.rpganimation.client.EyeRubRenderer.submit(partial, pose, collector, player)) {
            if(dev.shtark.rpganimation.client.ActivityAnimations.submitTypingHands(partial,pose,collector,player))return;
            double lowering = Math.max(dev.shtark.rpganimation.client.IntroController.handLowering(partial),
                    dev.shtark.rpganimation.client.ActivityAnimations.typingHandoff(partial));
            pose.pushPose();
            try {
                pose.translate(0, -1.5 * lowering, 0);
                pose.rotateDegrees(com.mojang.math.Axis.XP, (float)(12 * lowering));
                original.call(renderer, partial, pose, collector, player, hands);
                dev.shtark.rpganimation.client.BasicAnimations.swimmingSupport(player, pose, collector);
            } finally { pose.popPose(); }
        }
    }
}
