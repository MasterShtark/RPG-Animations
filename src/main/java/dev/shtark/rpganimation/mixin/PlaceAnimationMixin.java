package dev.shtark.rpganimation.mixin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
abstract class PlaceAnimationMixin {
    @Inject(method="place",at=@At("RETURN"))
    private void rpgAnimation$placed(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> ci) {
        if(ci.getReturnValue().consumesAction()&&context.getPlayer() instanceof ServerPlayer p)
            dev.shtark.rpganimation.BasicActionServer.start(p,dev.shtark.rpganimation.animation.BasicAction.PLACE,context.getHand());
    }
}
