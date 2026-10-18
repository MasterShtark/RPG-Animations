package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.BedAnimationServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerSleepMixin {
    @Unique private Vec3 rpgAnimation$beforeBed;
    @Unique private float rpgAnimation$beforeYaw;
    @Unique private float rpgAnimation$beforePitch;
    @Inject(method = "startSleeping", at = @At("HEAD"))
    private void rpgAnimation$capture(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        var player = (ServerPlayer) (Object) this;
        rpgAnimation$beforeBed = player.isSleeping() ? null : player.position();
        rpgAnimation$beforeYaw = player.getYRot();
        rpgAnimation$beforePitch = player.getXRot();
    }
    @Inject(method = "startSleeping", at = @At("RETURN"))
    private void rpgAnimation$accepted(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && rpgAnimation$beforeBed != null)
            BedAnimationServer.begin((ServerPlayer) (Object) this, rpgAnimation$beforeBed,
                    rpgAnimation$beforeYaw, rpgAnimation$beforePitch);
        rpgAnimation$beforeBed = null;
    }

    @Unique private BedAnimationServer.ExitStart rpgAnimation$exitStart;
    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void rpgAnimation$captureExit(boolean force, boolean update, CallbackInfo callback) {
        rpgAnimation$exitStart = BedAnimationServer.captureExit((ServerPlayer)(Object)this);
    }
    @Inject(method = "stopSleepInBed", at = @At("RETURN"))
    private void rpgAnimation$exit(boolean force, boolean update, CallbackInfo callback) {
        BedAnimationServer.exit((ServerPlayer)(Object)this, rpgAnimation$exitStart);
        rpgAnimation$exitStart = null;
    }
}
