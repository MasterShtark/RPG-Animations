package dev.shtark.rpganimation.client.compat;

import dev.shtark.rpganimation.client.ClientConfig;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** Read-only optional adapters. No physics, death, revive or camera state is changed. */
public final class RagdollCompatibility {
    private static boolean initialized, scanned;
    private static Method sarosActive,sarosPlayer,ragdollHidden;
    private static final Set<Integer> SAROS_PLAYERS=new HashSet<>();
    private RagdollCompatibility() {}
    public static void clearFrame(){scanned=false;SAROS_PLAYERS.clear();}
    private static void initialize() {
        if(initialized)return;initialized=true;
        if(ModList.get().isLoaded("saros_ragdoll"))try {
            var type=Class.forName("de.sarocesch.sarosragdoll.ragdoll.RagdollEntity");
            sarosActive=type.getMethod("activeIn",Level.class);
            sarosPlayer=type.getMethod("getHandledPlayerId");
        } catch(ReflectiveOperationException|LinkageError e){warn("Saros Player Ragdoll",e);}
        if(ModList.get().isLoaded("ragdollified"))try {
            ragdollHidden=Class.forName("com.raiiiden.ragdollified.api.RagdollifiedApi").getMethod("isEntityHidden",Entity.class);
        } catch(ReflectiveOperationException|LinkageError e){warn("Ragdollified",e);}
    }
    public static boolean owns(Object player) {
        if(!(player instanceof Entity entity))return false;
        initialize();
        if(ClientConfig.SAROS_HANDOFF.get()&&sarosActive!=null)try {
            if(!scanned) {
                scanned=true;
                for(Object body:(Iterable<?>)sarosActive.invoke(null,entity.level())) {
                    int id=((Number)sarosPlayer.invoke(body)).intValue();
                    if(id>=0)SAROS_PLAYERS.add(id);
                }
            }
            if(SAROS_PLAYERS.contains(entity.getId()))return true;
        } catch(ReflectiveOperationException|LinkageError|RuntimeException e){sarosActive=null;SAROS_PLAYERS.clear();warn("Saros Player Ragdoll",e);}
        if(ClientConfig.RAGDOLLIFIED_HANDOFF.get()&&ragdollHidden!=null)try {
            return Boolean.TRUE.equals(ragdollHidden.invoke(null,entity));
        } catch(ReflectiveOperationException|LinkageError|RuntimeException e){ragdollHidden=null;warn("Ragdollified",e);}
        return false;
    }
    private static void warn(String mod,Throwable e) {
        com.mojang.logging.LogUtils.getLogger().warn("RPG Animation cannot read {} ragdoll ownership; its adapter is disabled for this session. Check the installed version.",mod,e);
    }
}
