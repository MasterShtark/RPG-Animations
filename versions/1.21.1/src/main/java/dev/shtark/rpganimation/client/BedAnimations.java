package dev.shtark.rpganimation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.shtark.rpganimation.BedAnimationPayload;
import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.BedEntrySequence;
import dev.shtark.rpganimation.animation.BedExitSequence;
import dev.shtark.rpganimation.animation.MotionCurve;
import net.minecraft.core.Direction;
import dev.shtark.rpganimation.animation.BedGeometry;
import dev.shtark.rpganimation.animation.PlaybackSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;

import dev.shtark.rpganimation.client.pose.PlayerFrame;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import dev.shtark.rpganimation.client.pose.PoseKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import dev.shtark.rpganimation.client.pose.PoseSampler;
import dev.shtark.rpganimation.client.pose.PoseRegistry;

@EventBusSubscriber(modid = RpgAnimation.ID, value = Dist.CLIENT)
public final class BedAnimations {
    public static final PoseKey<Double> TIME = new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "bed_time"));
    public static final PoseKey<BedGeometry.Scene> POSE = new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "bed_pose"));
    public static final PoseKey<String> ACTION=new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"bed_action"));
    private static final PoseKey<BedGeometry.Transform> TRANSFORM = new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "bed_transform"));
    private record Run(BedAnimationPayload payload, PlaybackSnapshot clock) {}
    private static final Map<UUID, Run> RUNNING = new HashMap<>();
    private static long ticks;
    private static ResourceKey<Level> dimension;

    private static void checkDimension() {
        var level = Minecraft.getInstance().level;
        ResourceKey<Level> current = level == null ? null : level.dimension();
        if (!java.util.Objects.equals(current, dimension)) { RUNNING.clear(); dimension = current; }
    }
    public static void accept(BedAnimationPayload payload) {
        checkDimension();
        if (!payload.active() || payload.elapsed() < 0 || payload.elapsed() >= duration(payload) || payload.facing() < 0 || payload.facing() > 3
                || !finite(payload.origin()) || !finite(payload.bed()) || !Float.isFinite(payload.yaw())
                || !Float.isFinite(payload.pitch()) || payload.origin().distanceToSqr(payload.bed()) > 64) {
            RUNNING.remove(payload.player());
        } else RUNNING.put(payload.player(), new Run(payload, new PlaybackSnapshot(payload.elapsed(), ticks)));
    }
    private static boolean finite(Vec3 v) { return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z); }
    private static Double time(UUID id, double partial) {
        if (!ClientConfig.BED_ANIMATION.get() || ClientConfig.REDUCED_MOTION.get()) return null;
        var run = RUNNING.get(id);
        if (run == null) return null;
        double time = run.clock.time(ticks, partial);
        return time < duration(run.payload) ? time : null;
    }
    public static boolean isRunning(UUID id) { return time(id, 0) != null; }
    private static int duration(BedAnimationPayload p) {
        return p.exiting() ? (p.reluctant() && ClientConfig.RELUCTANT_WAKE.get() ? BedExitSequence.DURATION+16 : BedExitSequence.DURATION) : dev.shtark.rpganimation.animation.AnimationTempo.ENTRY_DURATION;
    }
    private static double phase(BedAnimationPayload p, double age) {
        return p.exiting() ? (p.reluctant() && ClientConfig.RELUCTANT_WAKE.get() ? BedExitSequence.reluctantPhase(age) : BedExitSequence.phase(age)) : dev.shtark.rpganimation.animation.AnimationTempo.entry(age);
    }
    private static Direction direction(BedAnimationPayload p) {
        return switch (p.facing()) { case 1 -> Direction.EAST; case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST; default -> Direction.NORTH; };
    }
    private static Run localRun(float partial) {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.isAlive() || time(p.getUUID(), partial) == null) return null;
        var run = RUNNING.get(p.getUUID());
        if (p.isSleeping() == run.payload.exiting()) return null;
        if (run.payload.exiting() && (p.hurtTime > 0 || p.isPassenger()
                || p.position().distanceToSqr(run.payload.origin()) > .04)) {
            RUNNING.remove(p.getUUID());
            return null;
        }
        return run;
    }
    public static BedGeometry.Scene localScene(float partial) {
        var p = Minecraft.getInstance().player;
        var run = localRun(partial);
        return run == null ? null : scene(run.payload, direction(run.payload),
                p.getEyeHeight(Pose.STANDING), p.getScale(), run.clock.time(ticks, partial));
    }
    public static double localUpright(float partial) {
        var run = localRun(partial);
        return run == null ? 0 : 1;
    }
    public static Vec3 cameraPosition(Vec3 vanilla, float partial) {
        var run = localRun(partial);
        if (run == null) return vanilla;
        double age = run.clock.time(ticks, partial);
        var frame = BedEntrySequence.sample(phase(run.payload, age));
        var eye = localScene(partial).eye();
        Vec3 sleeping = run.payload.exiting() ? run.payload.bed().add(0, .35, 0) : vanilla;
        var position = new Vec3(eye.x(), eye.y(), eye.z()).lerp(sleeping, frame.recline());
        return run.payload.exiting() ? position.lerp(vanilla, MotionCurve.ease((age - (duration(run.payload) - 12)) / 12)) : position;
    }
    public record CameraAngles(float yaw, float pitch) {}

    // Called after vanilla's sleeping branch, which overwrites ComputeCameraAngles.
    public static CameraAngles cameraAngles(float vanillaYaw, float vanillaPitch, float partial) {
        var mc = Minecraft.getInstance();
        if (mc.getCameraEntity() != mc.player || !mc.options.getCameraType().isFirstPerson()) return null;
        var playback = localRun(partial);
        if (playback == null) return null;
        var run = playback.payload;
        double age = playback.clock.time(ticks, partial);
        var f = BedEntrySequence.sample(phase(run, age));
        var pose = localScene(partial);
        double yaw = 180 - pose.transform().yaw();
        double sleepYaw = run.exiting() ? run.facing() * 90 : vanillaYaw;
        yaw += BedGeometry.angleDelta(yaw, sleepYaw) * f.recline();
        double pitch = (run.pitch() * (1 - f.turn()) + 72 * f.support()) * (1 - f.recline());
        if (!run.exiting()) pitch += vanillaPitch * f.recline();
        else {
            double finish = MotionCurve.ease((age - (duration(run) - 12)) / 12);
            yaw += BedGeometry.angleDelta(yaw, vanillaYaw) * finish;
            pitch += (vanillaPitch - pitch) * finish;
        }
        return new CameraAngles((float)yaw, (float)pitch);
    }
    private static BedGeometry.Scene scene(BedAnimationPayload p, net.minecraft.core.Direction d,
                                           double eyeHeight, double scale, double age) {
        double angle = switch (d) { case SOUTH -> 90; case WEST -> 0; case NORTH -> 270; case EAST -> 180; default -> 0; };
        var result = BedGeometry.scene(p.origin().x, p.origin().y, p.origin().z, p.bed().x, p.bed().y, p.bed().z,
                p.yaw(), angle, d.getStepX(), d.getStepZ(), eyeHeight, scale, phase(p,age));
        if(p.exiting()&&p.reluctant()&&ClientConfig.RELUCTANT_WAKE.get()) {
            double rub=BedExitSequence.faceRub(age);
            return new BedGeometry.Scene(result.transform(),result.hip(),result.eye(),result.leadLeg(),result.trailLeg(),
                    result.arm()*(1-rub)+(-2.45+.12*Math.sin((age-12)*.8))*rub,
                    result.head()+.18*rub,result.leftArm());
        }
        return result;
    }
    public static String localActionKey() {
        var p=Minecraft.getInstance().player;if(p==null||!ClientConfig.BED_ANIMATION.get()||time(p.getUUID(),0)==null)return null;
        return RUNNING.get(p.getUUID()).payload.exiting()?"bed_exit":"bed_entry";
    }
    public static void register(PoseRegistry event) {
        event.registerAvatarEntityModifier(new PoseSampler() {
            @Override
            public void accept(net.minecraft.world.entity.LivingEntity avatar, PlayerFrame state) {
                state.setRenderData(ACTION,null);
                state.setRenderData(TIME, null); state.setRenderData(TRANSFORM, null); state.setRenderData(POSE, null);
                state.setRenderData(BedArmRenderer.HIDE_LOCAL_BODY, null);
                Double age = time(avatar.getUUID(), state.partialTick);
                if (age == null || state.deathTime > 0) return;
                var p = RUNNING.get(avatar.getUUID()).payload;
                String action=p.exiting()?"bed_exit":"bed_entry";state.setRenderData(ACTION,action);
                if(!dev.shtark.rpganimation.client.compat.AnimationCompatibility.bodyAllowed(state,action))return;
                if (state.hasPose(Pose.SLEEPING) == p.exiting()) return;
                // Vanilla's bed offset uses standing eye height, even while sleeping.
                var scene = scene(p, direction(p), avatar.getEyeHeight(Pose.STANDING), state.scale, age);
                var t = scene.transform();
                state.setRenderData(TIME, age);
                state.setRenderData(POSE, scene);
                state.setRenderData(TRANSFORM, new BedGeometry.Transform(t.x() + p.bed().x - state.x,
                        t.y() + p.bed().y - state.y, t.z() + p.bed().z - state.z, t.yaw(), t.tilt()));
                // Only extracted render state changes; vanilla retains the logical sleep/standing state.
                state.pose = Pose.STANDING;
            }
        });
    }
    public static boolean transform(PlayerFrame state, PoseStack pose, float scale) {
        var t = state.getRenderData(TRANSFORM);
        if (t == null) return false;
        pose.translate(t.x() / scale, t.y() / scale, t.z() / scale);
        pose.mulPose(Axis.YP.rotationDegrees((float)t.yaw()));
        pose.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(t.tilt())));
        return true;
    }
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        checkDimension();
        if (!Minecraft.getInstance().isPaused()) ticks++;
        RUNNING.values().removeIf(run -> run.clock.time(ticks, 0) >= duration(run.payload));
    }
    @SubscribeEvent
    public static void clone(ClientPlayerNetworkEvent.Clone event) { RUNNING.remove(event.getOldPlayer().getUUID()); }
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { RUNNING.clear(); dimension = null; ticks = 0; }
}
