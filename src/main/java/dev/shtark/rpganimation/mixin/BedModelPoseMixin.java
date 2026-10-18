package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.ObserverPose;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after subclass animation hooks, immediately before deferred model drawing. */
@Mixin(ModelFeatureRenderer.class)
abstract class BedModelPoseMixin {
    @Inject(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void rpgAnimation$bedPose(ModelFeatureRenderer.Submit<?> submit, CallbackInfo callback) {
        ObserverPose.finishBedPose(submit.model(), submit.state());
    }
}
