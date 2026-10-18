package dev.shtark.rpganimation;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ActivityBatchPayload(List<ActivityPayload> updates) implements CustomPacketPayload {
    public ActivityBatchPayload {updates=List.copyOf(updates);if(updates.size()>128)throw new IllegalArgumentException("Activity batch too large");}
    public static final Type<ActivityBatchPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"activity_batch"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ActivityBatchPayload> CODEC=StreamCodec.of(
        (b,p)->{b.writeVarInt(p.updates.size());for(var update:p.updates)ActivityPayload.CODEC.encode(b,update);},
        b->{int n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("Invalid activity batch size");
            var list=new ArrayList<ActivityPayload>(n);for(int i=0;i<n;i++)list.add(ActivityPayload.CODEC.decode(b));return new ActivityBatchPayload(list);});
    @Override public Type<ActivityBatchPayload> type(){return TYPE;}
}
