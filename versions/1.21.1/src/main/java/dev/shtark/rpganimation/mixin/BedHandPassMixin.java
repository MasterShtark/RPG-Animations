package dev.shtark.rpganimation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.client.*;
import dev.shtark.rpganimation.client.compat.AnimationCompatibility;
import dev.shtark.rpganimation.client.pose.PoseRegistry;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=GameRenderer.class,priority=2000)
abstract class BedHandPassMixin {
    @Inject(method="renderItemInHand",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$bed(CallbackInfo ci) {
        if(BedAnimations.localUpright(0)>0&&AnimationCompatibility.localPresentationAllowed())ci.cancel();
    }
    @WrapOperation(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
    private void rpgAnimation$hands(ItemInHandRenderer renderer,float partial,PoseStack pose,MultiBufferSource.BufferSource buffer,LocalPlayer player,int light,Operation<Void> original) {
        var frame=PoseRegistry.get(player,partial);
        if(EyeRubRenderer.submit(partial,pose,buffer,frame)||ActivityAnimations.submitTypingHands(partial,pose,buffer,frame))return;
        double lowering=Math.max(IntroController.handLowering(partial),ActivityAnimations.typingHandoff(partial));
        pose.pushPose();
        try {
            pose.translate(0,-1.5*lowering,0);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float)(12*lowering)));
            original.call(renderer,partial,pose,buffer,player,light);
            BasicAnimations.swimmingSupport(frame,pose,buffer);
        } finally {pose.popPose();}
    }
}
