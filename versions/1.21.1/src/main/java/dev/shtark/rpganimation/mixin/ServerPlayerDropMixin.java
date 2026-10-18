package dev.shtark.rpganimation.mixin;

import dev.shtark.rpganimation.DropAnimationServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerDropMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("RETURN"))
    private void rpgAnimation$drop(ItemStack stack, boolean dropAround, boolean traceItem,
                                   CallbackInfoReturnable<ItemEntity> callback) {
        var dropped = callback.getReturnValue();
        if (!dropAround && traceItem && dropped != null && !dropped.getItem().isEmpty())
            DropAnimationServer.dropped((ServerPlayer)(Object)this);
    }
}
