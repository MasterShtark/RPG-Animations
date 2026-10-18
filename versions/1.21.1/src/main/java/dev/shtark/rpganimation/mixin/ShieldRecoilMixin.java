package dev.shtark.rpganimation.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(LivingEntity.class)
abstract class ShieldRecoilMixin {
    @WrapOperation(method="hurt",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/event/entity/living/LivingShieldBlockEvent;getBlockedDamage()F"))
    private float rpgAnimation$blocked(LivingShieldBlockEvent event,Operation<Float> original) {
        float blocked=original.call(event);
        if(blocked>0&&(Object)this instanceof ServerPlayer p)
            dev.shtark.rpganimation.BasicActionServer.start(p,dev.shtark.rpganimation.animation.BasicAction.SHIELD_HIT,p.getUsedItemHand());
        return blocked;
    }
}
