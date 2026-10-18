package dev.shtark.rpganimation.client.compat;

import dev.shtark.rpganimation.animation.GuiBounds;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

/** Explicit backpack-owned UI bounds; no whole-screen capture or arbitrary widget introspection. */
public final class BackpackPreview {
    private static boolean sophisticatedFailed,travelerFailed;
    private static Method tabs,tabRectangles,upgradeRectangle,sortRectangle,widgetBounds,slotBounds,slotHidden;
    private static Field widgets,slots;
    private BackpackPreview() {}
    public static GuiBounds extend(Screen screen,GuiBounds base,boolean sophisticated) {
        if(sophisticated?sophisticatedFailed:travelerFailed)return base;
        try {
            GuiBounds result=base;
            if(sophisticated) {
                if(tabs==null) {
                    var type=Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase");
                    tabs=type.getMethod("getUpgradeSettingsControl");upgradeRectangle=type.getMethod("getUpgradeSlotsRectangle");sortRectangle=type.getMethod("getSortButtonsRectangle");
                    tabRectangles=Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabControl").getMethod("getTabRectangles");
                }
                Object control=tabs.invoke(screen);
                if(control!=null)for(Object rect:(Iterable<?>)tabRectangles.invoke(control))result=result.union(rectangle((Rect2i)rect));
                for(Method method:new Method[]{upgradeRectangle,sortRectangle}) {
                    var optional=(Optional<?>)method.invoke(screen);
                    if(optional.isPresent())result=result.union(rectangle((Rect2i)optional.get()));
                }
            } else {
                if(widgets==null) {
                    var type=Class.forName("com.tiviacz.travelersbackpack.client.screens.BackpackScreen");
                    widgets=type.getField("mappedWidgets");slots=type.getField("upgradeSlots");
                    widgetBounds=Class.forName("com.tiviacz.travelersbackpack.client.screens.widgets.WidgetBase").getMethod("getWidgetSizeAndPos");
                    var slot=Class.forName("com.tiviacz.travelersbackpack.client.screens.UpgradeSlot");
                    slotBounds=slot.getMethod("getUpgradeSlotSizeAndPos");slotHidden=slot.getMethod("isHidden");
                }
                Object mapped=widgets.get(screen);
                if(mapped instanceof Map<?,?> map)for(Object widget:map.values())result=result.union(rectangle((int[])widgetBounds.invoke(widget)));
                Object upgrades=slots.get(screen);
                if(upgrades instanceof Iterable<?> list)for(Object slot:list)
                    if(!Boolean.TRUE.equals(slotHidden.invoke(slot)))result=result.union(rectangle((int[])slotBounds.invoke(slot)));
            }
            return result.clipped(screen.width,screen.height);
        } catch(ReflectiveOperationException|LinkageError|RuntimeException e) {
            if(sophisticated)sophisticatedFailed=true;else travelerFailed=true;
            com.mojang.logging.LogUtils.getLogger().warn("RPG Animation cannot read {} backpack side-panel bounds; using the main inventory window for this session.",sophisticated?"Sophisticated":"Traveler",e);
            return base;
        }
    }
    private static GuiBounds rectangle(Rect2i r){return new GuiBounds(r.getX(),r.getY(),r.getWidth(),r.getHeight());}
    private static GuiBounds rectangle(int[] r){
        if(r.length!=4)throw new IllegalArgumentException("Expected backpack GUI rectangle");
        return new GuiBounds(r[0],r[1],r[2],r[3]);
    }
}
