package dev.shtark.rpganimation;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(RpgAnimation.ID)
public final class RpgAnimation {
    public static final String ID = "rpg_animation";
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> INTRO_SEEN =
            ATTACHMENTS.register("intro_seen", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("seen").codec()).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<java.util.List<String>>> DISCOVERIES =
            ATTACHMENTS.register("discoveries", () -> AttachmentType.<java.util.List<String>>builder(() -> java.util.List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("items").codec()).copyOnDeath().build());

    public RpgAnimation(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,ServerConfig.SPEC);
        ATTACHMENTS.register(bus);
        bus.addListener(RpgAnimation::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("19");
        registrar.playToServer(PageTurnRequest.TYPE,PageTurnRequest.CODEC,(payload,context)->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)BasicActionServer.page(player);
        });
        registrar.playToClient(BasicActionPayload.TYPE, BasicActionPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToClient(SequencePayload.TYPE, SequencePayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToClient(ObserverPayload.TYPE, ObserverPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToClient(BedAnimationPayload.TYPE, BedAnimationPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToClient(DropPayload.TYPE, DropPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToClient(PickupPayload.TYPE, PickupPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToClient(InventoryPreviewPayload.TYPE, InventoryPreviewPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToServer(InventoryPreviewRequest.TYPE,InventoryPreviewRequest.CODEC,(payload,context)->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)InventoryPreviewServer.accept(player,payload);
        });
        registrar.playToClient(ActivityBatchPayload.TYPE, ActivityBatchPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToClient(PreviewPolicyPayload.TYPE, PreviewPolicyPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToServer(SubscriptionRequest.TYPE,SubscriptionRequest.CODEC,(payload,context)->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)ActivityServer.subscribe(player,payload);
        });
        registrar.playToServer(ActivityRequest.TYPE,ActivityRequest.CODEC,(payload,context)->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)ActivityServer.accept(player,payload);
        });
        registrar.playToClient(ContextPayload.TYPE, ContextPayload.CODEC, ClientPayloadDispatch::handle);
        registrar.playToServer(PocketRequest.TYPE, PocketRequest.CODEC, (payload,context)->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer player) ContextAnimationServer.pockets(player);
        });
        registrar.playToServer(PreviewPayload.TYPE, PreviewPayload.CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) IntroServer.preview(player);
                });
        registrar.playToServer(FinishPayload.TYPE, FinishPayload.CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) IntroServer.clientFinished(player);
                });
    }
}
