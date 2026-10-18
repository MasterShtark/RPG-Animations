package dev.shtark.rpganimation.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.shtark.rpganimation.FinishPayload;
import dev.shtark.rpganimation.PreviewPayload;
import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.WakeUpSequence;
import dev.shtark.rpganimation.animation.EyeRubSequence;
import dev.shtark.rpganimation.mixin.ClientInputAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = RpgAnimation.ID, value = Dist.CLIENT)
public final class IntroController {
    private static final Identifier EYELID_TEXTURE = Identifier.fromNamespaceAndPath(
            RpgAnimation.ID, "textures/gui/eyelid.png");
    private static final int EYELID_TINT = 0xFF687C70;
    private static boolean pending;
    private static boolean active;
    private static int elapsed;
    private static int waitTicks;
    private static int returnTicks;
    private static double returnStart;
    private static CameraType previousPerspective;
    private static Vec3 origin;

    public static void request() {
        finish(false);
        pending = true;
        waitTicks = 0;
    }

    public static boolean active() { return active; }

    private static double timelineTime(double partialTick) {
        var player = Minecraft.getInstance().player;
        Double synchronizedTime = player == null ? null : ObserverAnimations.time(player.getUUID(), partialTick);
        return synchronizedTime == null ? Math.max(0, elapsed - 1 + partialTick) : synchronizedTime;
    }

    public static EyeRubSequence.Frame eyeRubFrame(double partialTick) {
        return EyeRubSequence.sample(timelineTime(partialTick), ClientConfig.REDUCED_MOTION.get());
    }

    public static double handLowering(double partialTick) {
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("wake_up")
                || ClientConfig.REDUCED_MOTION.get()) return 0;
        if (active) return WakeUpSequence.handLowering(timelineTime(partialTick));
        double t = Math.max(0, Math.min(1, (returnTicks - partialTick) / 8.0));
        return returnStart * t * t * (3 - 2 * t);
    }

    public static WakeUpSequence.Frame frame(double partialTick) {
        return WakeUpSequence.sample(timelineTime(partialTick), ClientConfig.REDUCED_MOTION.get());
    }

    public static void finish(boolean notifyServer) {
        var mc = Minecraft.getInstance();
        boolean wasRunning = active || pending;
        double lowering = active ? handLowering(0) : 0;
        returnTicks = notifyServer && active && mc.player != null && mc.player.isAlive() ? 8 : 0;
        returnStart = lowering;
        if (previousPerspective != null) mc.options.setCameraType(previousPerspective);
        previousPerspective = null;
        active = pending = false;
        elapsed = waitTicks = 0;
        origin = null;
        if (notifyServer && wasRunning && mc.getConnection() != null)
            ClientPacketDistributor.sendToServer(FinishPayload.INSTANCE);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { returnTicks = 0; returnStart = 0; }
        else if (!mc.isPaused() && returnTicks > 0) returnTicks--;
        if (!active && !pending) return;
        if (mc.player == null || mc.level == null) { finish(false); return; }
        // A first-person owner must not cancel the separate third-person server timeline.
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("wake_up")) { finish(false); return; }
        if (!ClientConfig.ENABLED.get() || !mc.player.isAlive() || mc.player.isPassenger()
                || mc.player.isInWater() || mc.player.isInLava() || mc.player.isSpectator()
                || mc.player.isSleeping() || mc.player.hurtTime > 0) { finish(true); return; }
        if (pending) {
            if (++waitTicks > 40) { finish(true); return; }
            if (mc.gui.screen() != null || !mc.player.onGround()) return;
            pending = false;
            active = true;
            elapsed = 0;
            origin = mc.player.position();
            previousPerspective = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (mc.gui.screen() != null || !mc.options.getCameraType().isFirstPerson()
                || origin.distanceToSqr(mc.player.position()) > 0.5 || !mc.player.onGround()) {
            finish(true);
            return;
        }
        if (!mc.isPaused() && ++elapsed >= WakeUpSequence.DURATION) finish(true);
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles event) {
        if (!active || !dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("wake_up")) return;
        var frame = frame(event.getPartialTick());
        event.setPitch(Math.min(85, event.getPitch() + frame.pitchOffset()));
        event.setRoll(event.getRoll() + frame.roll());
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post event) {
        if (!active || !dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("wake_up")) return;
        var mc = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        var frame = frame(event.getPartialTick().getGameTimeDeltaPartialTick(true));
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int alpha = Math.round(frame.darkness() * 255);
        if (alpha > 0) graphics.fill(0, 0, width, height, alpha << 24);
        if (frame.eyelids() > 0) {
            // Square contour steps and enlarged texels match Minecraft's blocky style.
            int step = Math.max(3, (int) Math.ceil(width / 80.0));
            int textureWidth = width * 2;
            int textureScale = Math.max(width, height) * 2;
            float rimV = textureScale * 0.5F;
            for (int x = 0; x < width; x += step) {
                int column = Math.min(step, width - x);
                double horizontal = 2.0 * (x + column * 0.5) / width - 1;
                int solid = WakeUpSequence.blockyLidHeight(horizontal, frame.eyelids(), height, step);
                if (solid <= 0) continue;
                graphics.blit(RenderPipelines.GUI_TEXTURED, EYELID_TEXTURE,
                        x, 0, x, rimV - solid, column, solid, column, solid,
                        textureWidth, textureScale, EYELID_TINT);
                graphics.blit(RenderPipelines.GUI_TEXTURED, EYELID_TEXTURE,
                        x, height - solid, x, rimV, column, solid, column, -solid,
                        textureWidth, textureScale, EYELID_TINT);
                int rim = Math.min(solid, step);
                graphics.fill(x, solid - rim, x + column, solid, 0x50181818);
                graphics.fill(x, height - solid, x + column, height - solid + rim, 0x50181818);
            }
        }
        var text = Component.translatable("rpg_animation.intro.skip");
        graphics.text(mc.font, text, (width - mc.font.width(text)) / 2, height - 28, 0xFFD5D5D5);
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        if (!active || event.getEntity() != Minecraft.getInstance().player) return;
        event.getInput().keyPresses = Input.EMPTY;
        ((ClientInputAccessor) event.getInput()).rpgAnimation$setMoveVector(Vec2.ZERO);
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active) { event.setCanceled(true); event.setSwingHand(false); }
    }

    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        if (active && event.getAction() == InputConstants.PRESS
                && (event.getKey() == InputConstants.KEY_SPACE || event.getKey() == InputConstants.KEY_ESCAPE)) finish(true);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { finish(false); }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rpganimation")
                .then(Commands.literal("preview").executes(context -> {
                    if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("wake_up") || !ClientConfig.ENABLED.get()) return 0;
                    ClientPacketDistributor.sendToServer(PreviewPayload.INSTANCE);
                    return 1;
                }))
                .then(Commands.literal("skip").executes(context -> { finish(true); return 1; })));
    }
}


