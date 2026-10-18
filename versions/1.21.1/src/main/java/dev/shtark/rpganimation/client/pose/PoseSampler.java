package dev.shtark.rpganimation.client.pose;

import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface PoseSampler {
    void accept(LivingEntity player, PlayerFrame frame);
}
