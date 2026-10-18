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
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = RpgAnimation.ID, dist = Dist.CLIENT)
public final class RpgAnimationClient {
    public RpgAnimationClient(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        bus.addListener(RpgAnimationClient::payloads);
        bus.addListener(RpgAnimationClient::setup);
        bus.addListener(dev.shtark.rpganimation.client.compat.AnimationCompatibility::register);
        bus.addListener(ObserverAnimations::register);
        bus.addListener(BedAnimations::register);
        bus.addListener(DropAnimations::register);
        bus.addListener(PickupAnimations::register);
        bus.addListener(BasicAnimations::register);
        bus.addListener(ContextAnimations::register);
        bus.addListener(ActivityAnimations::register);
        bus.addListener(BedArmRenderer::reload);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("firstperson")) FirstPersonCompat.register();
        });
    }

    private static void payloads(RegisterClientPayloadHandlersEvent event) {
        event.register(dev.shtark.rpganimation.InventoryPreviewPayload.TYPE,(payload,context)->InventoryScreenPreview.accept(payload));
        event.register(dev.shtark.rpganimation.ActivityBatchPayload.TYPE,(payload,context)->payload.updates().forEach(ActivityAnimations::accept));
        event.register(dev.shtark.rpganimation.PreviewPolicyPayload.TYPE,(payload,context)->InventoryScreenPreview.policy(payload));
        event.register(dev.shtark.rpganimation.BasicActionPayload.TYPE,(payload,context)->BasicAnimations.accept(payload));
        event.register(ContextPayload.TYPE, (payload,context)->ContextAnimations.accept(payload));
        event.register(PickupPayload.TYPE, (payload, context) -> PickupAnimations.accept(payload));
        event.register(DropPayload.TYPE, (payload, context) -> DropAnimations.accept(payload));
        event.register(BedAnimationPayload.TYPE, (payload, context) -> BedAnimations.accept(payload));
        event.register(ObserverPayload.TYPE, (payload, context) -> ObserverAnimations.accept(payload));
        event.register(SequencePayload.TYPE, (payload, context) -> {
            if (payload.start()) IntroController.request();
            else IntroController.finish(false);
        });
    }
}
