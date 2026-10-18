package dev.shtark.rpganimation.mixin;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LecternMenu.class)
abstract class LecternAnimationMixin {
    @Shadow public abstract int getPage();
    @Unique private int rpgAnimation$oldPage;
    @Inject(method="clickMenuButton",at=@At("HEAD"))
    private void rpgAnimation$before(Player player,int button,CallbackInfoReturnable<Boolean> ci){rpgAnimation$oldPage=getPage();}
    @Inject(method="clickMenuButton",at=@At("RETURN"))
    private void rpgAnimation$page(Player player,int button,CallbackInfoReturnable<Boolean> ci){
        if(ci.getReturnValueZ()&&getPage()!=rpgAnimation$oldPage&&player instanceof ServerPlayer p)
            dev.shtark.rpganimation.BasicActionServer.start(p,dev.shtark.rpganimation.animation.BasicAction.PAGE,net.minecraft.world.InteractionHand.MAIN_HAND);
    }
}
