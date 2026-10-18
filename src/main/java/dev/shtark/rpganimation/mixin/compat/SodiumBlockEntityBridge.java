package dev.shtark.rpganimation.mixin.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve Sodium's block-entity extraction hook across the removed frustum parameter. */
@Mixin(value=LevelExtractor.class,priority=900)
public abstract class SodiumBlockEntityBridge {
    @Shadow private void extractVisibleBlockEntities(Camera camera,float partial,LevelRenderState state){throw new AssertionError();}

    // Intentionally named: Sodium 0.9.2 injects into this exact four-argument descriptor.
    // Priority below Sodium allows its injector to target this merged overload.
    private void extractVisibleBlockEntities(Camera camera,float partial,LevelRenderState state,Frustum frustum) {
        extractVisibleBlockEntities(camera,partial,state);
    }

    @Redirect(method="extract",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/extract/LevelExtractor;extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/level/LevelRenderState;)V"))
    private void rpgAnimation$rendererBridge(LevelExtractor extractor,Camera camera,float partial,LevelRenderState state) {
        extractVisibleBlockEntities(camera,partial,state,camera.getCullFrustum());
    }
}
