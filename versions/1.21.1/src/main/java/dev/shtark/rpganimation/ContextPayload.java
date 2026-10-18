package dev.shtark.rpganimation;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ContextPayload(UUID player,int kind,int elapsed,int variant,boolean active,double x,double y,double z) implements CustomPacketPayload {
    public ContextPayload(UUID player,int kind,int elapsed,int variant,boolean active){this(player,kind,elapsed,variant,active,0,0,0);}
    public static final Type<ContextPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"context_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ContextPayload> CODEC=StreamCodec.of((b,p)->{
        b.writeUUID(p.player);b.writeVarInt(p.kind);b.writeVarInt(p.elapsed);b.writeVarInt(p.variant);b.writeBoolean(p.active);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);
    },b->new ContextPayload(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble()));
    @Override public Type<ContextPayload> type(){return TYPE;}
}
