package dev.shtark.rpganimation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SequencePayload(boolean start) implements CustomPacketPayload {
    public static final Type<SequencePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "sequence"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SequencePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SequencePayload::start, SequencePayload::new);
    @Override public Type<SequencePayload> type() { return TYPE; }
}
