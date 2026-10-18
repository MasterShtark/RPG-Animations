package dev.shtark.rpganimation.client;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import dev.shtark.rpganimation.animation.GuiBounds;
import dev.shtark.rpganimation.client.compat.BackpackPreview;
/** Only explicitly supported content windows; never capture arbitrary screens or chat. */
public final class PreviewScreens {
    private PreviewScreens() {}
    public static GuiBounds bounds(Screen screen) {
        if(screen instanceof AbstractContainerScreen<?> container) {
            String name=screen.getClass().getName();
            boolean sophisticated=name.equals("net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen");
            boolean traveler=name.equals("com.tiviacz.travelersbackpack.client.screens.BackpackScreen");
            if((sophisticated&&!ClientConfig.SOPHISTICATED_PREVIEWS.get())||(traveler&&!ClientConfig.TRAVELER_PREVIEWS.get()))return null;
            var base=new GuiBounds(container.getLeftPos(),container.getTopPos(),container.getImageWidth(),container.getImageHeight()).clipped(screen.width,screen.height);
            return sophisticated||traveler?BackpackPreview.extend(screen,base,sophisticated):base;
        }
        if(ClientConfig.BOOK_PREVIEWS.get()&&(screen instanceof BookViewScreen||screen instanceof BookEditScreen))
            return new GuiBounds((screen.width-192)/2,2,192,192).clipped(screen.width,screen.height);
        return null;
    }
}
