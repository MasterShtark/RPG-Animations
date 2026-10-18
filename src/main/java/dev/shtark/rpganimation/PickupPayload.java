package dev.shtark.rpganimation;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PickupPayload(UUID player, int elapsed, boolean active) implements CustomPacketPayload {
    public static final Type<PickupPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID, "pickup_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PickupPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.player());
                buffer.writeVarInt(payload.elapsed());
                buffer.writeBoolean(payload.active());
            }, buffer -> new PickupPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readBoolean()));
    @Override public Type<PickupPayload> type() { return TYPE; }
}
