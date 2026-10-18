package dev.shtark.rpganimation.client.pose;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import dev.shtark.rpganimation.RpgAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** One sample per player per rendered frame, shared by model and hand passes. */
@EventBusSubscriber(modid=RpgAnimation.ID,value=Dist.CLIENT)
public final class PoseRegistry {
    public static final PoseRegistry INSTANCE = new PoseRegistry();
    private final List<PoseSampler> samplers = new ArrayList<>();
    private final Map<AbstractClientPlayer,PlayerFrame> frames = new IdentityHashMap<>();
    private PoseRegistry() {}
    public void registerAvatarEntityModifier(PoseSampler sampler) { samplers.add(sampler); }
    public static PlayerFrame get(AbstractClientPlayer player, float partial) {
        if(player==null)return null;
        return INSTANCE.frames.computeIfAbsent(player,p->{
            var frame = new PlayerFrame(p,partial);
            for(var sampler:INSTANCE.samplers)sampler.accept(p,frame);
            return frame;
        });
    }
    public static PlayerFrame local(float partial) { return get(Minecraft.getInstance().player,partial); }
    @SubscribeEvent public static void begin(RenderFrameEvent.Pre event) { INSTANCE.frames.clear(); }
    @SubscribeEvent public static void end(RenderFrameEvent.Post event) { INSTANCE.frames.clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { INSTANCE.frames.clear(); }
}
