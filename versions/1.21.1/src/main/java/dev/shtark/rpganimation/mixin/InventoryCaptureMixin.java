package dev.shtark.rpganimation.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.shtark.rpganimation.client.InventoryScreenPreview;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class InventoryCaptureMixin {

    @Inject(method="render",at=@At("TAIL"))
    private void rpgAnimation$captureInventory(CallbackInfo ci){InventoryScreenPreview.capture(net.minecraft.client.Minecraft.getInstance().getMainRenderTarget());}
}
