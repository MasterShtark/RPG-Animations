package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.BasicActionServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class BasicMountMixin {
    @Inject(method="startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",at=@At("RETURN"))
    private void rpgAnimation$mount(Entity vehicle,boolean force,boolean events,CallbackInfoReturnable<Boolean> ci) {
        if(ci.getReturnValueZ()&&(Object)this instanceof ServerPlayer p)BasicActionServer.mounted(p,vehicle);
    }
}
