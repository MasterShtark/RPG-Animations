package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.ActivityAnimations;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class ActivityTabMixin {
    @Inject(method="getNameForDisplay",at=@At("RETURN"),cancellable=true)
    private void rpgAnimation$status(PlayerInfo info,CallbackInfoReturnable<Component> cir) {
        var suffix=ActivityAnimations.tab(info.getProfile().id());
        if(suffix!=null)cir.setReturnValue(cir.getReturnValue().copy().append(suffix));
    }
}
