package dev.shtark.rpganimation;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned cosmetic work and bandwidth limits. */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CONTEXT,ACTIVITY,PREVIEWS,BASIC_ACTIONS,SOPHISTICATED_BACKPACKS,TRAVELERS_BACKPACK,TELEPORT_RESET,UNKNOWN_MOUNT_ANIMATIONS;
    public static final ModConfigSpec.IntValue PREVIEW_WIDTH,PREVIEW_INTERVAL,PREVIEW_RECIPIENTS,PREVIEW_BYTES,ACTIVITY_RANGE;
    public static final java.util.Map<dev.shtark.rpganimation.animation.ContextClip,ModConfigSpec.BooleanValue> GESTURES=new java.util.EnumMap<>(dev.shtark.rpganimation.animation.ContextClip.class);
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SEAT_TYPES;
    static {
        var b=new ModConfigSpec.Builder();
        b.translation("rpg_animation.configuration.server").push("serverFeatures");
        UNKNOWN_MOUNT_ANIMATIONS=b.comment("Use generic RPG mounting/dismounting on unrecognised creatures. Disabled protects custom riding behaviour, including Alex's Mobs Continued. Vanilla mount profiles remain enabled; generic poses are not fitted to every creature.").translation("rpg_animation.configuration.server.unknownMounts").define("unknownMountAnimations",false);
        TELEPORT_RESET=b.comment("Clear cosmetic gestures and motion history after successful position-changing teleports, including Waystones. Does not change travel mechanics.").translation("rpg_animation.configuration.server.teleportReset").define("resetAnimationsOnTeleport",true);
        BASIC_ACTIONS=b.comment("Enable cosmetic basic actions and successful interaction notifications; does not change gameplay.").translation("rpg_animation.configuration.server.basic").define("basicActions",true);
        SOPHISTICATED_BACKPACKS=b.comment("Observe Sophisticated worn backpacks, including slots registered by its integrations, for cosmetic equip gestures.").translation("rpg_animation.configuration.server.sophisticatedBackpacks").define("sophisticatedBackpackAnimations",true);
        TRAVELERS_BACKPACK=b.comment("Observe Traveler worn backpacks through its attachment/accessory integration for cosmetic equip gestures.").translation("rpg_animation.configuration.server.travelersBackpack").define("travelersBackpackAnimations",true);
        CONTEXT=b.comment("Enable automatic contextual gestures and their environmental checks.").translation("rpg_animation.configuration.server.context").define("contextAnimations",true);
        ACTIVITY=b.comment("Enable player activity sharing. Disabling clears remote status.").translation("rpg_animation.configuration.server.activity").define("activity",true);
        PREVIEWS=b.comment("Permit opt-in inventory image sharing. Client consent is still required.").translation("rpg_animation.configuration.server.previews").define("inventoryPreviews",true);
        ACTIVITY_RANGE=b.comment("Maximum range for detailed activity poses, in blocks. Tab status remains global for subscribers.").translation("rpg_animation.configuration.server.range").defineInRange("activityRange",24,4,64);
        b.pop().translation("rpg_animation.configuration.server.bandwidth").push("previewLimits");
        PREVIEW_WIDTH=b.comment("Maximum preview width; height remains bounded to96. Lower values reduce traffic.").translation("rpg_animation.configuration.server.width").defineInRange("maxWidth",96,32,128);
        PREVIEW_INTERVAL=b.comment("Minimum ticks between captures/uploads.20 ticks is one second at20 TPS.").translation("rpg_animation.configuration.server.interval").defineInRange("intervalTicks",40,20,100);
        PREVIEW_RECIPIENTS=b.comment("Maximum nearby subscribers per shared image.").translation("rpg_animation.configuration.server.recipients").defineInRange("maxRecipients",8,1,16);
        PREVIEW_BYTES=b.comment("Aggregate image payload bytes forwarded per20 server ticks, before compression. Excess updates are deferred.").translation("rpg_animation.configuration.server.bytes").defineInRange("bytesPerSecond",262144,32768,4194304);
        b.pop().translation("rpg_animation.configuration.server.gestures").push("gestures");
        for(var clip:dev.shtark.rpganimation.animation.ContextClip.values())
            GESTURES.put(clip,b.translation("rpg_animation.configuration.context."+clip.name().toLowerCase(java.util.Locale.ROOT))
                    .define(clip.name().toLowerCase(java.util.Locale.ROOT),true));
        b.pop().push("seatCompatibility");
        SEAT_TYPES=b.comment("Seat entity registry IDs. Only explicitly listed mounts receive seat transitions; never ordinary animals or boats.")
                .defineListAllowEmpty("entityTypes",java.util.List.of("sit:entity_sit"),()->"sit:entity_sit",value->value instanceof String id&&net.minecraft.resources.ResourceLocation.tryParse(id)!=null);
        b.pop();SPEC=b.build();
    }
    private ServerConfig() {}
}
