package dev.shtark.rpganimation.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemInHandRenderer.class)
public interface FishingHandTransforms {
    @Invoker("applyItemArmTransform") void rpgAnimation$equip(PoseStack pose,HumanoidArm arm,float height);
    @Invoker("applyItemArmAttackTransform") void rpgAnimation$attack(PoseStack pose,HumanoidArm arm,float attack);
    @Accessor("mainHandHeight") float rpgAnimation$mainHeight();
    @Accessor("oMainHandHeight") float rpgAnimation$oldMainHeight();
    @Accessor("offHandHeight") float rpgAnimation$offHeight();
    @Accessor("oOffHandHeight") float rpgAnimation$oldOffHeight();
}
