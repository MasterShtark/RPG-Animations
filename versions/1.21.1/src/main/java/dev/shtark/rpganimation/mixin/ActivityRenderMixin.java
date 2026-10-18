package dev.shtark.rpganimation.mixin;
import dev.shtark.rpganimation.client.ActivityAnimations;
import dev.shtark.rpganimation.client.pose.PoseRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerRenderer.class)
abstract class ActivityRenderMixin {
    @Inject(method="render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("TAIL"))
    private void rpgAnimation$activity(AbstractClientPlayer player,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light,CallbackInfo ci) {
        ActivityAnimations.draw(PoseRegistry.get(player,partial),pose,buffer);
    }
}
