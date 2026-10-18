package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.BasicAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class BasicHandMotionMixin {
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method="renderPlayerArm",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private float rpgAnimation$emptyHandSwing(float attack) {
        return BasicAnimations.replaceSwing()?0:attack;
    }
    @Inject(method="swingArm",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$swing(float animation,PoseStack pose,int invert,HumanoidArm arm,CallbackInfo ci) {
        if(BasicAnimations.replaceSwing())ci.cancel();
    }
    @Inject(method="applyEatTransform",at=@At("HEAD"),cancellable=true)
    private void rpgAnimation$eat(PoseStack pose,float partial,HumanoidArm arm,float remaining,int duration,CallbackInfo ci) {
        if(BasicAnimations.replaceEating())ci.cancel();
    }
}
