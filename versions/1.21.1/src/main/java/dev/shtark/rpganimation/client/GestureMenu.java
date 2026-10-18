package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.ContextClip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Client menu sends ordinary authenticated self-gesture commands, not arbitrary player IDs. */
@EventBusSubscriber(modid=RpgAnimation.ID,value=Dist.CLIENT)
public final class GestureMenu extends Screen {
    private static final ContextClip[] CHOICES={ContextClip.WAVE,ContextClip.NOD,ContextClip.BOW,ContextClip.BECKON,ContextClip.POINT,ContextClip.THANKS,ContextClip.CAMPFIRE_REST};
    public GestureMenu(){super(Component.translatable("rpg_animation.emotes"));}
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("rpganimation")
                .then(net.minecraft.commands.Commands.literal("menu").executes(c->{Minecraft.getInstance().execute(()->Minecraft.getInstance().setScreen(new GestureMenu()));return 1;})));
    }
    @Override protected void init(){
        int left=width/2-124,top=Math.max(30,height/2-70);
        for(int i=0;i<CHOICES.length;i++){
            var clip=CHOICES[i];String key=clip.name().toLowerCase(java.util.Locale.ROOT);
            var button=Button.builder(Component.translatable("rpg_animation.emote."+key),b->{
                if(minecraft.player!=null)minecraft.player.connection.sendCommand("rpganimation gesture "+key);
                onClose();
            }).bounds(left+(i%2)*126,top+(i/2)*26,122,22).build();
            button.active=ClientConfig.CONTEXT.get(clip).get()&&!ClientConfig.REDUCED_MOTION.get()
                    &&dev.shtark.rpganimation.ServerConfig.GESTURES.get(clip).get();
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.translatable("rpg_animation.emote.close"),b->onClose()).bounds(left,top+110,248,20).build());
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){
        super.render(graphics,x,y,partial);
        graphics.drawString(font,title,(width-font.width(title))/2,Math.max(12,height/2-93),0xffffffff);
    }
    @Override public boolean isPauseScreen(){return false;}
}
