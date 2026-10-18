package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.InventoryPreview;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record InventoryPreviewRequest(int revision,int width,int height,byte[] pixels) implements CustomPacketPayload {
    public static final Type<InventoryPreviewRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"inventory_preview_request"));
    private static final dev.shtark.rpganimation.animation.ByteBudget DECODE_BUDGET=new dev.shtark.rpganimation.animation.ByteBudget();
    public static final StreamCodec<RegistryFriendlyByteBuf,InventoryPreviewRequest> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.revision);b.writeVarInt(p.width);b.writeVarInt(p.height);b.writeByteArray(p.pixels);},
            b->{
                int revision=b.readVarInt(),width=b.readVarInt(),height=b.readVarInt(),length=b.readVarInt();
                if(revision<0||!InventoryPreview.valid(width,height,length)||length>b.readableBytes())throw new IllegalArgumentException("Invalid preview dimensions or length");
                // Bound aggregate allocation before constructing the byte array, including rejected senders.
                if(!DECODE_BUDGET.take(System.nanoTime()/1_000_000_000L,length,4*1024*1024)) {
                    b.skipBytes(length);return new InventoryPreviewRequest(0,-2,0,new byte[0]);
                }
                byte[] data=new byte[length];b.readBytes(data);return new InventoryPreviewRequest(revision,width,height,data);
            });
    @Override public Type<InventoryPreviewRequest> type(){return TYPE;}
}
