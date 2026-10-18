package dev.shtark.rpganimation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
/** No page text or contents are transmitted. Server validates held book and rate. */
public record PageTurnRequest() implements CustomPacketPayload {
    public static final Type<PageTurnRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"page_turn"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PageTurnRequest> CODEC=StreamCodec.unit(new PageTurnRequest());
    @Override public Type<PageTurnRequest> type(){return TYPE;}
}
