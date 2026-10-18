package dev.shtark.rpganimation;

import java.util.UUID;
import dev.shtark.rpganimation.animation.InventoryPreview;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record InventoryPreviewPayload(UUID player,int width,int height,byte[] pixels) implements CustomPacketPayload {
    public static final Type<InventoryPreviewPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"inventory_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf,InventoryPreviewPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.player);b.writeVarInt(p.width);b.writeVarInt(p.height);b.writeByteArray(p.pixels);},
            b->new InventoryPreviewPayload(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readByteArray(InventoryPreview.MAX_BYTES)));
    @Override public Type<InventoryPreviewPayload> type(){return TYPE;}
}
