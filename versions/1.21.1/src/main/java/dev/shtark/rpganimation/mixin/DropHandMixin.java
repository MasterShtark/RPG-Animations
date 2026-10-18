package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.client.DropAnimations;
import dev.shtark.rpganimation.client.PickupAnimations;
import dev.shtark.rpganimation.client.ContextAnimations;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import dev.shtark.rpganimation.client.pose.PlayerFrame;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class DropHandMixin {
    // Transform inside vanilla's balanced per-hand push/pop; the offhand stays untouched.
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void rpgAnimation$drop(net.minecraft.client.player.AbstractClientPlayer entity,
                                  float partial, float pitch, InteractionHand hand, float attack, ItemStack item,
                                  float height, PoseStack pose, MultiBufferSource collector, int light, CallbackInfo callback) {
        var player = dev.shtark.rpganimation.client.pose.PoseRegistry.get(entity,partial);
        dev.shtark.rpganimation.client.BasicAnimations.resetHandPass();
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed()) return;
        // The line uses vanilla's rod anchor. Pickup, gait and generic swing must
        // preserve it too, not only the dedicated fishing clip.
        if(ContextAnimations.nativeRodHand(hand))return;
        if(dev.shtark.rpganimation.client.BasicAnimations.firstPerson(player,hand,pose))return;
        DropAnimations.firstPerson(player, hand, pose);
        if(player==null||player.getRenderData(ContextAnimations.FRAME)==null)
            PickupAnimations.firstPerson(player, hand, pose);
        ContextAnimations.firstPerson(player,hand,pose,height);
    }
    @Inject(method="renderArmWithItem",at=@At("RETURN"))
    private void rpgAnimation$endHandPass(net.minecraft.client.player.AbstractClientPlayer entity,
            float partial, float pitch, InteractionHand hand, float attack, ItemStack item,
            float height, PoseStack pose, MultiBufferSource collector, int light, CallbackInfo callback) {
        var player = dev.shtark.rpganimation.client.pose.PoseRegistry.get(entity,partial);
        dev.shtark.rpganimation.client.BasicAnimations.resetHandPass();
    }
}
