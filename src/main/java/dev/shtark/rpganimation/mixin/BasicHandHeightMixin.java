package dev.shtark.rpganimation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.shtark.rpganimation.client.BasicAnimations;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FirstPersonHandsAndItems.class)
abstract class BasicHandHeightMixin {
    // Only remove cooldown-driven lowering. Vanilla still lowers/replaces actual
    // item swaps, busy hands and used items through its existing height state.
    @WrapOperation(method="tick", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float rpgAnimation$attackHeight(LocalPlayer player, float partial, Operation<Float> original) {
        float nativeScale = original.call(player, partial);
        return BasicAnimations.ownsAttackHeight(player) ? 1.0F : nativeScale;
    }
}
