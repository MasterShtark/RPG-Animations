package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.BedAnimations;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class BedCullingMixin {
    @Inject(method = "getBoundingBoxForCulling(Lnet/minecraft/world/entity/LivingEntity;F)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true)
    private void rpgAnimation$bounds(LivingEntity entity, float partial, CallbackInfoReturnable<AABB> callback) {
        if (BedAnimations.isRunning(entity.getUUID()) || dev.shtark.rpganimation.client.BasicAnimations.mounting(entity.getUUID()))
            callback.setReturnValue(callback.getReturnValue().inflate(8));
    }
}
