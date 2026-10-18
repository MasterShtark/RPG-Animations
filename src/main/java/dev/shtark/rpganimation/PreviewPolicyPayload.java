package dev.shtark.rpganimation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PreviewPolicyPayload(boolean allowed,int width,int interval) implements CustomPacketPayload {
    public static final Type<PreviewPolicyPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"preview_policy"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PreviewPolicyPayload> CODEC=StreamCodec.of(
        (b,p)->{b.writeBoolean(p.allowed);b.writeVarInt(p.width);b.writeVarInt(p.interval);},
        b->new PreviewPolicyPayload(b.readBoolean(),b.readVarInt(),b.readVarInt()));
    @Override public Type<PreviewPolicyPayload> type(){return TYPE;}
}
