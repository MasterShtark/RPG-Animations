package dev.shtark.rpganimation.client;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import dev.shtark.rpganimation.client.pose.PlayerFrame;
import dev.shtark.rpganimation.animation.EyeRubSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;

/** Uses the player's existing skin and arm geometry; never changes inventory or player pose. */
public final class EyeRubRenderer {
    private EyeRubRenderer() {}

    /** Returns true when the intro owns the entire hand submission for this pass. */
    public static boolean submit(float partialTick, PoseStack pose, MultiBufferSource collector, PlayerFrame state) {
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("wake_up") || !IntroController.active()) return false;
        if (ClientConfig.REDUCED_MOTION.get() || IntroController.handLowering(partialTick) < 1) return false;
        var avatar = state;
        if (avatar == null) return false;
        var motion = IntroController.eyeRubFrame(partialTick);
        if (motion.lift() > 0 && !avatar.isInvisible) {
            PlayerRenderer renderer = (PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(avatar.player);
            arm(pose, collector, avatar, renderer, avatar.mainArm, motion);
        }
        return true;
    }

    private static void arm(PoseStack pose, MultiBufferSource collector,
                            dev.shtark.rpganimation.client.pose.PlayerFrame avatar, PlayerRenderer renderer,
                            HumanoidArm arm, EyeRubSequence.Frame motion) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.pushPose();
        try {
            pose.translate(side * (0.14 - 0.65 * motion.horizontal()),
                    -1.55 + 0.98 * motion.lift() + 0.65 * motion.vertical(), -0.32);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 8));
            pose.mulPose(Axis.XP.rotationDegrees(-4));
            // Recenter vanilla arm pivots before positioning the shoulders below the view.
            pose.translate(side * 0.3125, -0.125, 0);
            var texture = avatar.skin.texture();
            if (arm == HumanoidArm.RIGHT) {
                renderer.renderRightHand(pose, collector, avatar.lightCoords,
                        avatar.player);
            } else {
                renderer.renderLeftHand(pose, collector, avatar.lightCoords,
                        avatar.player);
            }
        } finally {
            pose.popPose();
        }
    }
}
