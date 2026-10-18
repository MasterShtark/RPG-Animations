package dev.shtark.rpganimation.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface FishingHandRendererAccess {
    @Accessor("firstPersonHandsAndItemsRenderer")
    FirstPersonHandsAndItemsRenderer rpgAnimation$hands();
}
