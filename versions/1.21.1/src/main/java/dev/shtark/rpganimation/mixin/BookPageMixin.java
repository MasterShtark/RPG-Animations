package dev.shtark.rpganimation.mixin;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({BookViewScreen.class, BookEditScreen.class})
abstract class BookPageMixin {
    @Shadow(remap = false) private int currentPage;
    @Unique private int rpgAnimation$oldPage;
    @Inject(method={"pageForward","pageBack"},at=@At("HEAD"))
    private void rpgAnimation$before(CallbackInfo ci){rpgAnimation$oldPage=currentPage;}
    @Inject(method={"pageForward","pageBack"},at=@At("RETURN"))
    private void rpgAnimation$page(CallbackInfo ci){if(currentPage!=rpgAnimation$oldPage)dev.shtark.rpganimation.client.BasicAnimations.pageChanged();}
}
