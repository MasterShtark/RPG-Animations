package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.client.BedAnimations;
import dev.shtark.rpganimation.client.pose.PoseRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
abstract class BedRendererMixin {
    @Inject(method="setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$bed(AbstractClientPlayer player,PoseStack pose,float bob,float yaw,float partial,float scale,CallbackInfo ci) {
        var state=PoseRegistry.get(player,partial);
        if(!dev.shtark.rpganimation.client.compat.AnimationCompatibility.bodyAllowed(state))return;
        dev.shtark.rpganimation.client.BasicAnimations.mountTransform(state,pose,scale);
        dev.shtark.rpganimation.client.ContextAnimations.seatTransform(state,pose,scale);
        if(BedAnimations.transform(state,pose,scale))ci.cancel();
    }
}
