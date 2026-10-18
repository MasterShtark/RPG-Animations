package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.TeleportAnimations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class TeleportAnimationMixin {
    @Unique private Vec3 rpgAnimation$beforeTravel;
    @Unique private Object rpgAnimation$travelLevel;
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("HEAD"))
    private void rpgAnimation$before(ServerLevel level,double x,double y,double z,Set<RelativeMovement> relatives,float yaw,float pitch,CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer player=(ServerPlayer)(Object)this;
        rpgAnimation$beforeTravel=player.position();
        rpgAnimation$travelLevel=player.level();
    }
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("RETURN"))
    private void rpgAnimation$after(ServerLevel level,double x,double y,double z,Set<RelativeMovement> relatives,float yaw,float pitch,CallbackInfoReturnable<Boolean> callback) {
        ServerPlayer player=(ServerPlayer)(Object)this;
        boolean moved=rpgAnimation$beforeTravel!=null && (rpgAnimation$travelLevel!=player.level() || rpgAnimation$beforeTravel.distanceToSqr(player.position())>0.0001);
        rpgAnimation$beforeTravel=null;
        rpgAnimation$travelLevel=null;
        if(callback.getReturnValueZ() && moved) TeleportAnimations.reset(player);
    }
}
