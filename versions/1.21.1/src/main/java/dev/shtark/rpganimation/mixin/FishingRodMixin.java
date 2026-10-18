package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.ContextAnimationServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
public abstract class FishingRodMixin {
    @Inject(method="use",at=@At("RETURN"))
    private void rpgAnimation$rodUsed(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack>> ci) {
        if(player instanceof ServerPlayer server)ContextAnimationServer.fishingUsed(server,hand);
    }
}

