package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.SequencePayload;
import dev.shtark.rpganimation.ObserverPayload;
import dev.shtark.rpganimation.BedAnimationPayload;
import dev.shtark.rpganimation.DropPayload;
import dev.shtark.rpganimation.PickupPayload;
import dev.shtark.rpganimation.ContextPayload;
import dev.shtark.rpganimation.client.compat.FirstPersonCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(value = RpgAnimation.ID, dist = Dist.CLIENT)
public final class RpgAnimationClient {
    public RpgAnimationClient(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        dev.shtark.rpganimation.ClientPayloadDispatch.receiver = RpgAnimationClient::payload;
        bus.addListener(RpgAnimationClient::setup);
        dev.shtark.rpganimation.client.compat.AnimationCompatibility.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        ObserverAnimations.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        BedAnimations.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        DropAnimations.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        PickupAnimations.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        BasicAnimations.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        ContextAnimations.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        ActivityAnimations.register(dev.shtark.rpganimation.client.pose.PoseRegistry.INSTANCE);
        bus.addListener(BedArmRenderer::reload);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("firstperson")) FirstPersonCompat.register();
        });
    }

    private static void payload(net.minecraft.network.protocol.common.custom.CustomPacketPayload message) {
        if (message instanceof dev.shtark.rpganimation.InventoryPreviewPayload payload) InventoryScreenPreview.accept(payload);
        else if (message instanceof dev.shtark.rpganimation.ActivityBatchPayload payload) payload.updates().forEach(ActivityAnimations::accept);
        else if (message instanceof dev.shtark.rpganimation.PreviewPolicyPayload payload) InventoryScreenPreview.policy(payload);
        else if (message instanceof dev.shtark.rpganimation.BasicActionPayload payload) BasicAnimations.accept(payload);
        else if (message instanceof ContextPayload payload) ContextAnimations.accept(payload);
        else if (message instanceof PickupPayload payload) PickupAnimations.accept(payload);
        else if (message instanceof DropPayload payload) DropAnimations.accept(payload);
        else if (message instanceof BedAnimationPayload payload) BedAnimations.accept(payload);
        else if (message instanceof ObserverPayload payload) ObserverAnimations.accept(payload);
        else if (message instanceof SequencePayload payload) {
            if (payload.start()) IntroController.request();
            else IntroController.finish(false);
        }
    }
}
