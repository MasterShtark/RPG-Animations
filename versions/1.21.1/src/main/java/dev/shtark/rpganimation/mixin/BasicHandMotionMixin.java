package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.BasicAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class BasicHandMotionMixin {
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method="renderArmWithItem",at=@At("HEAD"),argsOnly=true,ordinal=2)
    private float rpgAnimation$swing(float swing) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        var frame=dev.shtark.rpganimation.client.pose.PoseRegistry.local(mc.getTimer().getGameTimeDeltaPartialTick(false));
        return frame!=null&&dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed()
            &&BasicAnimations.replaceBodySwing(frame)?0:swing;
    }
    @Inject(method="applyEatTransform",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$eat(PoseStack pose,float partial,HumanoidArm arm,ItemStack stack,Player player,CallbackInfo ci) {
        if(BasicAnimations.replaceEating())ci.cancel();
    }
}
