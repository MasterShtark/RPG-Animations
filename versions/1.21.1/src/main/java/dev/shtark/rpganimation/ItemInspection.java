package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.ContextClip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

/** Tag/component-driven classification, evaluated only on server inspection attempts. */
final class ItemInspection {
    private static final TagKey<Item> SPECIAL=TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"special_inspection"));
    private ItemInspection() {}
    static ContextClip style(ItemStack item) {
        var id=BuiltInRegistries.ITEM.getKey(item.getItem());
        boolean special=item.is(SPECIAL)||item.isEnchanted()||item.getRarity()==Rarity.RARE||item.getRarity()==Rarity.EPIC
                ||(id.getNamespace().equals("minecraft")&&id.getPath().startsWith("netherite_"));
        if(item.getItem() instanceof BlockItem)return ContextClip.INSPECT;
        if(item.is(ItemTags.SWORDS))return special?ContextClip.INSPECT_SPECIAL_SWORD:ContextClip.INSPECT_SWORD;
        if(item.is(ItemTags.AXES))return special?ContextClip.INSPECT_SPECIAL_TOOL:ContextClip.INSPECT_AXE;
        if(item.is(ItemTags.PICKAXES))return special?ContextClip.INSPECT_SPECIAL_TOOL:ContextClip.INSPECT_PICKAXE;
        if(item.is(ItemTags.SHOVELS))return special?ContextClip.INSPECT_SPECIAL_TOOL:ContextClip.INSPECT_SHOVEL;
        if(item.is(ItemTags.HOES))return special?ContextClip.INSPECT_SPECIAL_TOOL:ContextClip.INSPECT_HOE;
        if(net.minecraft.world.item.Equipable.get(item)!=null)return special?ContextClip.INSPECT_SPECIAL_GEAR:ContextClip.INSPECT_GEAR;
        return special?ContextClip.INSPECT_TREASURE:ContextClip.INSPECT;
    }
}
