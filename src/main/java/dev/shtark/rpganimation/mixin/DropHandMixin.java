package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.client.DropAnimations;
import dev.shtark.rpganimation.client.PickupAnimations;
import dev.shtark.rpganimation.client.ContextAnimations;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class DropHandMixin {
    // Transform inside vanilla's balanced per-hand push/pop; the offhand stays untouched.
    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void rpgAnimation$drop(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state,
                                  float partial, float pitch, InteractionHand hand, float attack, ItemStack item,
                                  float height, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo callback) {
        dev.shtark.rpganimation.client.BasicAnimations.resetHandPass();
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed()) return;
        // The line uses vanilla's rod anchor. Pickup, gait and generic swing must
        // preserve it too, not only the dedicated fishing clip.
        if(ContextAnimations.nativeRodHand(hand))return;
        if(dev.shtark.rpganimation.client.BasicAnimations.firstPerson(player.avatarRenderState,hand,pose))return;
        DropAnimations.firstPerson(player.avatarRenderState, hand, pose);
        if(player.avatarRenderState==null||player.avatarRenderState.getRenderData(ContextAnimations.FRAME)==null)
            PickupAnimations.firstPerson(player.avatarRenderState, hand, pose);
        ContextAnimations.firstPerson(player.avatarRenderState,hand,pose,height);
    }
    @Inject(method="submitArmWithItem",at=@At("RETURN"))
    private void rpgAnimation$endHandPass(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state,
            float partial, float pitch, InteractionHand hand, float attack, ItemStack item,
            float height, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo callback) {
        dev.shtark.rpganimation.client.BasicAnimations.resetHandPass();
    }
}
