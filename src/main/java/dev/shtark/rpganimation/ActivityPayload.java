package dev.shtark.rpganimation;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** One activity update carried inside an ActivityBatchPayload. */
public record ActivityPayload(UUID player,int kind,int intensity,long phase) {
    public static final StreamCodec<RegistryFriendlyByteBuf,ActivityPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.player);b.writeByte(p.kind);b.writeByte(p.intensity);b.writeLong(p.phase);},
            b->new ActivityPayload(b.readUUID(),b.readUnsignedByte(),b.readUnsignedByte(),b.readLong()));
}
