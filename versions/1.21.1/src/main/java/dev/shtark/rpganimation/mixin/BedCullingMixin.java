package dev.shtark.rpganimation.mixin;
import dev.shtark.rpganimation.client.BedAnimations;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(EntityRenderer.class)
abstract class BedCullingMixin {
    @WrapOperation(method="shouldRender",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getBoundingBoxForCulling()Lnet/minecraft/world/phys/AABB;"))
    private AABB rpgAnimation$bounds(Entity entity,Operation<AABB> original) {
        var bounds=original.call(entity);
        return BedAnimations.isRunning(entity.getUUID())||dev.shtark.rpganimation.client.BasicAnimations.mounting(entity.getUUID())?bounds.inflate(8):bounds;
    }
}
