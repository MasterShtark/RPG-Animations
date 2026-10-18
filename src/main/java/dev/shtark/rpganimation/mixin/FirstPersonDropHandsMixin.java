package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.ClientConfig;
import dev.shtark.rpganimation.client.DropAnimations;
import dev.shtark.rpganimation.client.PickupAnimations;
import dev.shtark.rpganimation.client.ContextAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional FPM 2.7.3 hand gate; its body activation API does not control this gate. */
@Pseudo
@Mixin(targets = "dev.tr7zw.firstperson.LogicHandler", remap = false)
abstract class FirstPersonDropHandsMixin {
    @Inject(method = "showVanillaHands()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void rpgAnimation$showDropHands(CallbackInfoReturnable<Boolean> callback) {
        if (dev.shtark.rpganimation.client.compat.FirstPersonCompat.presentation(false))
            callback.setReturnValue(true);
    }

    @Inject(method = "dynamicHandsEnabled()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void rpgAnimation$keepDropHandsInView(CallbackInfoReturnable<Boolean> callback) {
        if (dev.shtark.rpganimation.client.compat.FirstPersonCompat.presentation(false))
            callback.setReturnValue(false);
    }
}
