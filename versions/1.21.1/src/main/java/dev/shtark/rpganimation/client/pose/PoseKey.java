package dev.shtark.rpganimation.client.pose;

import net.minecraft.resources.ResourceLocation;

/** Typed key for RPG-owned data sampled during a render frame. */
public record PoseKey<T>(ResourceLocation id) {}
