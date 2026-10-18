package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface FishingHandTransforms {
    @Invoker("applyItemArmTransform")
    void rpgAnimation$equip(PoseStack pose, HumanoidArm arm, float height);
    @Invoker("swingArm")
    void rpgAnimation$swing(float attack, PoseStack pose, int side, HumanoidArm arm);
}
