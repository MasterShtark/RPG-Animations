package dev.shtark.rpganimation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubscriptionRequest(int generation,boolean activity,boolean tab,boolean previews,boolean sharingPreview,int distance) implements CustomPacketPayload {
    public static final Type<SubscriptionRequest> TYPE=new Type<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"subscriptions"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SubscriptionRequest> CODEC=StreamCodec.of(
        (b,p)->{b.writeVarInt(p.generation);b.writeBoolean(p.activity);b.writeBoolean(p.tab);b.writeBoolean(p.previews);b.writeBoolean(p.sharingPreview);b.writeVarInt(p.distance);},
        b->new SubscriptionRequest(b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt()));
    @Override public Type<SubscriptionRequest> type(){return TYPE;}
}
