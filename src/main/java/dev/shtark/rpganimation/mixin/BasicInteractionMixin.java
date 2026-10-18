package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.BasicActionServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
abstract class BasicInteractionMixin {
    @Unique private BlockState rpgAnimation$before;
    @Unique private AbstractContainerMenu rpgAnimation$menu;
    @Unique private boolean rpgAnimation$hoe;
    @Inject(method="useItemOn",at=@At("HEAD"))
    private void rpgAnimation$capture(ServerPlayer p,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci) {
        rpgAnimation$before=level.getBlockState(hit.getBlockPos());rpgAnimation$menu=p.containerMenu;
        rpgAnimation$hoe=stack.is(net.minecraft.tags.ItemTags.HOES);
    }
    @Inject(method="useItemOn",at=@At("RETURN"))
    private void rpgAnimation$success(ServerPlayer p,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci) {
        if(ci.getReturnValue().consumesAction()&&rpgAnimation$before!=null)
            BasicActionServer.used(p,hand,rpgAnimation$before,level.getBlockState(hit.getBlockPos()),p.containerMenu!=rpgAnimation$menu,rpgAnimation$hoe);
        rpgAnimation$before=null;rpgAnimation$menu=null;
    }
}
