package dev.shtark.rpganimation.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
abstract class ShieldRecoilMixin {
    @Inject(method="applyItemBlocking",at=@At("RETURN"))
    private void rpgAnimation$blocked(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Float> ci) {
        if(ci.getReturnValueF()>0&&(Object)this instanceof ServerPlayer p)
            dev.shtark.rpganimation.BasicActionServer.start(p,dev.shtark.rpganimation.animation.BasicAction.SHIELD_HIT,p.getUsedItemHand());
    }
}
