package dev.shtark.rpganimation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FinishPayload() implements CustomPacketPayload {
    public static final FinishPayload INSTANCE = new FinishPayload();
    public static final Type<FinishPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "finish"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FinishPayload> CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<FinishPayload> type() { return TYPE; }
}
