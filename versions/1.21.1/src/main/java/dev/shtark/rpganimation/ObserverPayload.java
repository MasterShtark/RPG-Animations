package dev.shtark.rpganimation;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ObserverPayload(UUID player, int elapsed, boolean active) implements CustomPacketPayload {
    public static final Type<ObserverPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "observer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ObserverPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.player());
                buffer.writeVarInt(payload.elapsed());
                buffer.writeBoolean(payload.active());
            }, buffer -> new ObserverPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readBoolean()));
    @Override public Type<ObserverPayload> type() { return TYPE; }
}
