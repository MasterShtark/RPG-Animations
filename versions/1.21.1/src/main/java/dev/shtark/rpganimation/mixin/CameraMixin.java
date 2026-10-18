package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.client.IntroController;
import dev.shtark.rpganimation.client.BedAnimations;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow public abstract Vec3 getPosition();
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    // Apply before Camera.update prepares its frustum, keeping culling and visuals consistent.
    @Inject(method = "setup", at = @At("TAIL"))
    private void rpgAnimation$lowerView(net.minecraft.world.level.BlockGetter level, net.minecraft.world.entity.Entity entity, boolean detached, boolean reverse, float partialTick, CallbackInfo callback) {
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed()) return;
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() == mc.player && BedAnimations.localUpright(partialTick) > 0 && mc.options.getCameraType().isFirstPerson()) {
            setPosition(BedAnimations.cameraPosition(getPosition(), partialTick));
            var angles = BedAnimations.cameraAngles(getYRot(), getXRot(), partialTick);
            if (angles != null) setRotation(angles.yaw(), angles.pitch());
            return;
        }
        if(mc.player!=null&&mc.getCameraEntity()==mc.player&&mc.options.getCameraType().isFirstPerson())
            setPosition(getPosition().add(dev.shtark.rpganimation.client.BasicAnimations.cameraOffset(partialTick)));
        if (!IntroController.active() || mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        double fraction = IntroController.frame(partialTick).eyeFraction();
        double feet = mc.player.yo + (mc.player.getY() - mc.player.yo) * partialTick;
        Vec3 current = getPosition();
        double height = Math.max(0.2, (current.y - feet) * fraction);
        setPosition(new Vec3(current.x, feet + height, current.z));
    }
}


