package dev.shtark.rpganimation.mixin;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface ChatInputAccessor {
    @Accessor("input") EditBox rpgAnimation$input();
}
