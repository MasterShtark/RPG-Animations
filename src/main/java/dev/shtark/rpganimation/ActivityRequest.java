package dev.shtark.rpganimation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ActivityRequest(int kind,int intensity) implements CustomPacketPayload {
    public static final Type<ActivityRequest> TYPE=new Type<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"activity_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ActivityRequest> CODEC=StreamCodec.of(
            (b,p)->{b.writeByte(p.kind);b.writeByte(p.intensity);},b->new ActivityRequest(b.readUnsignedByte(),b.readUnsignedByte()));
    @Override public Type<ActivityRequest> type(){return TYPE;}
}
