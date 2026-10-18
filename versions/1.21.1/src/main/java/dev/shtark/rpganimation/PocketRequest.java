package dev.shtark.rpganimation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record PocketRequest() implements CustomPacketPayload {
    public static final Type<PocketRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"pockets"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PocketRequest> CODEC=StreamCodec.unit(new PocketRequest());
    @Override public Type<PocketRequest> type(){return TYPE;}
}
