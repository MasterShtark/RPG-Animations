package dev.shtark.rpganimation.client.pose;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Snapshot for the entity-driven 1.21.1 renderer. Never changes the logical entity. */
public final class PlayerFrame extends HumanoidFrame {
    public final AbstractClientPlayer player;
    public final float partialTick, scale, eyeHeight, deathTime, walkAnimationPos, walkAnimationSpeed, bodyRot;
    public final double x, y, z, distanceToCameraSq;
    public final boolean isInvisible, showRightSleeve, showLeftSleeve;
    public final int id, lightCoords;
    public final PlayerSkin skin;
    public final Component nameTag;
    public Pose pose;

    public PlayerFrame(AbstractClientPlayer player, float partialTick) {
        this.player = player;
        this.partialTick = partialTick;
        mainArm = player.getMainArm();
        scale = player.getScale(); eyeHeight = player.getEyeHeight(); deathTime = player.deathTime;
        walkAnimationPos = player.walkAnimation.position(partialTick);
        walkAnimationSpeed = player.walkAnimation.speed(partialTick);
        bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        x = Mth.lerp(partialTick, player.xOld, player.getX());
        y = Mth.lerp(partialTick, player.yOld, player.getY());
        z = Mth.lerp(partialTick, player.zOld, player.getZ());
        var mc = Minecraft.getInstance();
        distanceToCameraSq = mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(x,y,z);
        isInvisible = player.isInvisible(); id = player.getId(); skin = player.getSkin(); pose = player.getPose();
        showRightSleeve = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        showLeftSleeve = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        nameTag = player.getDisplayName();
        lightCoords = mc.getEntityRenderDispatcher().getRenderer(player).getPackedLightCoords(player,partialTick);
    }
    public boolean hasPose(Pose candidate) { return pose == candidate; }
}
