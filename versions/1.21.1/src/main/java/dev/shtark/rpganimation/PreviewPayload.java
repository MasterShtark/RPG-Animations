package dev.shtark.rpganimation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PreviewPayload() implements CustomPacketPayload {
    public static final PreviewPayload INSTANCE = new PreviewPayload();
    public static final Type<PreviewPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID, "preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PreviewPayload> CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<PreviewPayload> type() { return TYPE; }
}
