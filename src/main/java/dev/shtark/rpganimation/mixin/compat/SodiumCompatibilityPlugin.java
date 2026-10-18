package dev.shtark.rpganimation.mixin.compat;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** This descriptor bridge is specific to the verified renderer/loader pair. */
public final class SodiumCompatibilityPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String pkg) {}
    @Override public String getRefMapperConfig(){return null;}
    @Override public boolean shouldApplyMixin(String target,String mixin) {
        var mods=net.neoforged.fml.loading.FMLLoader.getCurrent().getLoadingModList().getMods();
        return mods.stream().anyMatch(m->m.getModId().equals("sodium")&&m.getVersion().toString().equals("0.9.2+mc26.3"))
                &&mods.stream().anyMatch(m->m.getModId().equals("neoforge")&&m.getVersion().toString().equals("26.3.0.76"));
    }
    @Override public void acceptTargets(Set<String> mine,Set<String> others) {}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public void postApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
}
