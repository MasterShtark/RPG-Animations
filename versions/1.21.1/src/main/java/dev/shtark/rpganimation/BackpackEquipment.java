package dev.shtark.rpganimation;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Server-side read-only queries; optional slot integrations remain owned by each backpack mod. */
public final class BackpackEquipment {
    private static boolean initialized;
    private static Method provider,rendered,stack,traveler;
    private BackpackEquipment() {}
    private static void init() {
        if(initialized)return;initialized=true;
        if(ModList.get().isLoaded("sophisticatedbackpacks"))try {
            var type=Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider");
            provider=type.getMethod("get");rendered=type.getMethod("getBackpackFromRendered",Player.class);
            stack=Class.forName(type.getName()+"$RenderInfo").getMethod("getBackpack");
        } catch(ReflectiveOperationException|LinkageError e){provider=null;warn("Sophisticated Backpacks",e);}
        if(ModList.get().isLoaded("travelersbackpack"))try {
            traveler=Class.forName("com.tiviacz.travelersbackpack.capability.AttachmentUtils").getMethod("getWearingBackpack",Player.class);
        } catch(ReflectiveOperationException|LinkageError e){warn("Traveler's Backpack",e);}
    }
    public static String sophisticated(Player player) {
        init();if(provider==null||!ServerConfig.SOPHISTICATED_BACKPACKS.get())return "";
        try {
            var found=(Optional<?>)rendered.invoke(provider.invoke(null),player);
            return found.isEmpty()?"":key((ItemStack)stack.invoke(found.get()));
        } catch(ReflectiveOperationException|LinkageError|RuntimeException e){provider=null;warn("Sophisticated Backpacks",e);return "";}
    }
    public static String traveler(Player player) {
        init();if(traveler==null||!ServerConfig.TRAVELERS_BACKPACK.get())return "";
        try {return key((ItemStack)traveler.invoke(null,player));}
        catch(ReflectiveOperationException|LinkageError|RuntimeException e){traveler=null;warn("Traveler's Backpack",e);return "";}
    }
    private static String key(ItemStack item){return item.isEmpty()?"":BuiltInRegistries.ITEM.getKey(item.getItem()).toString();}
    private static void warn(String mod,Throwable e){com.mojang.logging.LogUtils.getLogger().warn("RPG Animation cannot read {} worn-backpack state; equip adapter disabled for this session.",mod,e);}
}

