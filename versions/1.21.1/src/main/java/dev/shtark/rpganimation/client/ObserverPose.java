package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.animation.EyeRubSequence;
import dev.shtark.rpganimation.animation.GetUpSequence;
import dev.shtark.rpganimation.animation.BedGeometry;
import net.minecraft.client.model.HumanoidModel;
import dev.shtark.rpganimation.client.pose.PlayerFrame;
import dev.shtark.rpganimation.client.pose.HumanoidFrame;
import net.minecraft.world.entity.HumanoidArm;

public final class ObserverPose {
    private ObserverPose() {}

    /** Reapply only the active bed pose after the complete model update returns. */
    public static void finishBedPose(Object candidate, Object state) {
        if (candidate instanceof HumanoidModel<?> model && state instanceof PlayerFrame avatar) {
            if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.bodyAllowed(avatar)) return;
            var pose = avatar.getRenderData(BedAnimations.POSE);
            if (pose != null) applyBed(model, pose);
            else {
                // The opening action blends over the already-smoothed browsing pose.
                // Its weight reaches zero before expiry, so there is no pose switch.
                ActivityAnimations.model(model,avatar);
                if(BasicAnimations.model(model,avatar))return;
                DropAnimations.model(model, avatar);
                if(avatar.getRenderData(ContextAnimations.FRAME)==null)PickupAnimations.model(model, avatar);
                ContextAnimations.model(model,avatar);
            }
        }
    }

    public static void apply(HumanoidModel<?> model, HumanoidFrame state) {
        if (!(state instanceof PlayerFrame avatar) || !dev.shtark.rpganimation.client.compat.AnimationCompatibility.bodyAllowed(avatar)) return;
        var bedPose = state.getRenderData(BedAnimations.POSE);
        if (bedPose != null) {
            applyBed(model, bedPose);
            return;
        }
        Double time = state.getRenderData(ObserverAnimations.TIME);
        if (time == null) return;
        var pose = GetUpSequence.sample(dev.shtark.rpganimation.animation.AnimationTempo.wake(time));
        float angle = (float) pose.pitch();
        var root = dev.shtark.rpganimation.client.pose.ModelRoot.of(model);
        // Anchor the pelvis rather than swinging the whole body around its feet.
        root.xRot = angle;
        root.zRot = (float) pose.roll();
        root.y = (float) (24 - pose.hipHeight() - 12 * Math.cos(angle));
        root.z = (float) (-12 * Math.sin(angle));
        model.rightLeg.xRot = (float) GetUpSequence.leadingLeg(pose);
        model.leftLeg.xRot = (float) GetUpSequence.supportLeg(pose);
        model.rightLeg.yRot = model.leftLeg.yRot = 0;
        model.rightLeg.zRot = 0;
        model.leftLeg.zRot = (float) GetUpSequence.supportLegRoll(pose);
        model.head.xRot = (float) pose.headPitch();
        model.head.yRot = 0;

        var rub = EyeRubSequence.sample(time, false);
        float lift = (float) rub.lift();
        model.rightArm.xRot = model.leftArm.xRot = 0;
        model.rightArm.yRot = model.leftArm.yRot = 0;
        model.rightArm.zRot = model.leftArm.zRot = 0;
        boolean right = state.mainArm == HumanoidArm.RIGHT;
        var support = right ? model.leftArm : model.rightArm;
        float planted = (float) GetUpSequence.plantedArm(pose);
        support.y += (float) GetUpSequence.shoulderDrop(pose);
        support.xRot = planted;
        var hand = right ? model.rightArm : model.leftArm;
        hand.y += (float) (GetUpSequence.shoulderDrop(pose) * (1 - lift));
        hand.xRot = (float) (-2.65 * lift + 0.65 * rub.vertical() + planted * (1 - lift));
        hand.zRot = (right ? 1 : -1) * (float) (0.40 * lift + 0.65 * rub.horizontal());
    }

    private static void applyBed(HumanoidModel<?> model, BedGeometry.Scene pose) {
        dev.shtark.rpganimation.client.pose.ModelRoot.of(model).resetPose();
        model.body.resetPose();
        model.head.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
        // The right leg mounts first; the trailing leg pushes off before following.
        model.rightLeg.xRot = (float) pose.leadLeg();
        model.leftLeg.xRot = (float) pose.trailLeg();
        model.rightLeg.yRot = model.leftLeg.yRot = 0;
        model.rightLeg.zRot = model.leftLeg.zRot = 0;
        model.rightArm.xRot = (float) pose.arm();
        model.leftArm.xRot = (float) pose.leftArm();
        model.rightArm.yRot = model.leftArm.yRot = 0;
        model.rightArm.zRot = model.leftArm.zRot = 0;
        model.body.xRot = model.body.yRot = model.body.zRot = 0;
        model.head.xRot = (float) pose.head();
        model.head.yRot = model.head.zRot = 0;
    }
}

