package dev.shtark.rpganimation.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.shtark.rpganimation.client.ContextAnimations;
import dev.shtark.rpganimation.client.compat.AnimationCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/** Anchor the local line using the same equip, swing and view-bob transforms as the rod. */
@Mixin(value=FishingHookRenderer.class,priority=900)
abstract class FishingLineViewMixin {
    // Apply after optional first-person endpoint replacements, including early returns.
    @ModifyReturnValue(method="getPlayerHandPos",at=@At("RETURN"))
    private Vec3 rpgAnimation$rodAnchor(Vec3 original,Player player,float vanillaSwing,float partial) {
        var mc=Minecraft.getInstance();var owner=mc.player;
        if(player!=owner||owner==null||!mc.options.getCameraType().isFirstPerson()||mc.getCameraEntity()!=owner
            ||owner.isScoping()||!ContextAnimations.fishingHands()||!AnimationCompatibility.localPresentationAllowed("fishing_hold"))return original;
        boolean main=owner.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST);
        var hand=main?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        var arm=main?owner.getMainArm():owner.getMainArm().getOpposite();int side=arm==HumanoidArm.RIGHT?1:-1;
        var transforms=(FishingHandTransforms)((FishingHandRendererAccess)mc.gameRenderer).rpgAnimation$hands();
        float equip=1-Mth.lerp(partial,main?transforms.rpgAnimation$oldMainHeight():transforms.rpgAnimation$oldOffHeight(),main?transforms.rpgAnimation$mainHeight():transforms.rpgAnimation$offHeight());
        float attack=(owner.swingingArm==null?InteractionHand.MAIN_HAND:owner.swingingArm)==hand?owner.getAttackAnim(partial):0;
        var rest=new PoseStack();transforms.rpgAnimation$equip(rest,arm,0);
        var animated=new PoseStack();
        animated.mulPose(Axis.XP.rotationDegrees((owner.getViewXRot(partial)-Mth.lerp(partial,owner.xBobO,owner.xBob))*.1f));
        animated.mulPose(Axis.YP.rotationDegrees((owner.getViewYRot(partial)-Mth.lerp(partial,owner.yBobO,owner.yBob))*.1f));
        animated.translate(side*(-.4f*Mth.sin(Mth.sqrt(attack)*(float)Math.PI)),.2f*Mth.sin(Mth.sqrt(attack)*(float)(Math.PI*2)),-.2f*Mth.sin(attack*(float)Math.PI));
        transforms.rpgAnimation$equip(animated,arm,equip);transforms.rpgAnimation$attack(animated,arm,attack);
        var camera=mc.gameRenderer.getMainCamera();
        var held=owner.getItemInHand(hand);
        var model=mc.getItemRenderer().getModel(held,owner.level(),owner,owner.getId());
        if(held.is(net.minecraft.world.item.Items.FISHING_ROD)&&!model.isCustomRenderer()) {
            // The vanilla cast sprite's tip is at pixel (14, 1.5). Apply the model's
            // actual display transform instead of vanilla's approximate screen offset.
            var display=arm==HumanoidArm.RIGHT?net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    :net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            model.getTransforms().getTransform(display).apply(arm==HumanoidArm.LEFT,animated);
            var tip=new Vector3f(14f/16f-.5f,14.5f/16f-.5f,0);
            animated.last().pose().transformPosition(tip);
            camera.rotation().transform(tip);
            return owner.getEyePosition(partial).add(tip.x,tip.y,tip.z);
        }
        var base=camera.getNearPlane().getPointOnPlane(side*.525f,-.1f).scale(960.0/mc.options.fov().get());
        var point=new Vector3f((float)base.x,(float)base.y,(float)base.z);
        new Quaternionf(camera.rotation()).conjugate().transform(point);
        new Matrix4f(rest.last().pose()).invert().transformPosition(point);animated.last().pose().transformPosition(point);
        camera.rotation().transform(point);
        return owner.getEyePosition(partial).add(point.x,point.y,point.z);
    }
}
