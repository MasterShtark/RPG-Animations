package dev.shtark.rpganimation;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BasicActionPayload(UUID player,int kind,int elapsed,boolean offhand,boolean active,double x,double y,double z) implements CustomPacketPayload {
    public static final Type<BasicActionPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"basic_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,BasicActionPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.player);b.writeVarInt(p.kind);b.writeVarInt(p.elapsed);b.writeBoolean(p.offhand);b.writeBoolean(p.active);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);
    },b->new BasicActionPayload(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble()));
    @Override public Type<BasicActionPayload> type(){return TYPE;}
}
