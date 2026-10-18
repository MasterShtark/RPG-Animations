package dev.shtark.rpganimation;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record BedAnimationPayload(UUID player, int elapsed, boolean active, Vec3 origin, Vec3 bed,
                                  float yaw, float pitch, boolean exiting, int facing, boolean reluctant) implements CustomPacketPayload {
    public static final Type<BedAnimationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "bed_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BedAnimationPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.player()); buffer.writeVarInt(payload.elapsed()); buffer.writeBoolean(payload.active());
                buffer.writeDouble(payload.origin.x); buffer.writeDouble(payload.origin.y); buffer.writeDouble(payload.origin.z);
                buffer.writeDouble(payload.bed.x); buffer.writeDouble(payload.bed.y); buffer.writeDouble(payload.bed.z);
                buffer.writeFloat(payload.yaw); buffer.writeFloat(payload.pitch);
                buffer.writeBoolean(payload.exiting); buffer.writeVarInt(payload.facing); buffer.writeBoolean(payload.reluctant);
            }, buffer -> new BedAnimationPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readBoolean(),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean()));
    public static BedAnimationPayload stop(UUID player) {
        return new BedAnimationPayload(player, 0, false, Vec3.ZERO, Vec3.ZERO, 0, 0, false, 0, false);
    }
    @Override public Type<BedAnimationPayload> type() { return TYPE; }
}
