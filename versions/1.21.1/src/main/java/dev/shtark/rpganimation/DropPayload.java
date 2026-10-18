package dev.shtark.rpganimation;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DropPayload(UUID player, int elapsed, boolean active) implements CustomPacketPayload {
    public static final Type<DropPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "drop_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DropPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.player());
                buffer.writeVarInt(payload.elapsed());
                buffer.writeBoolean(payload.active());
            }, buffer -> new DropPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readBoolean()));
    @Override public Type<DropPayload> type() { return TYPE; }
}
