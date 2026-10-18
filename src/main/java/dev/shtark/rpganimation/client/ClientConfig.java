package dev.shtark.rpganimation.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final java.util.Map<String,ModConfigSpec.EnumValue<dev.shtark.rpganimation.animation.AnimationPreference>> ACTION_OWNERS=new java.util.LinkedHashMap<>();
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ADDITIONAL_ANIMATION_OWNERS;
    public static final java.util.Map<dev.shtark.rpganimation.animation.BasicAction,ModConfigSpec.BooleanValue> BASIC=new java.util.EnumMap<>(dev.shtark.rpganimation.animation.BasicAction.class);
    public static final ModConfigSpec.BooleanValue WALKING,RUNNING,EATING;
    public static final ModConfigSpec.BooleanValue PUNCHY_HANDOFF, BETTER_COMBAT_HANDOFF, SAROS_HANDOFF, RAGDOLLIFIED_HANDOFF, CARRY_ON_HANDOFF, CORPSE_HANDOFF;
    public static final java.util.Map<dev.shtark.rpganimation.animation.ContextClip,ModConfigSpec.BooleanValue> CONTEXT =
            new java.util.EnumMap<>(dev.shtark.rpganimation.animation.ContextClip.class);
    public static final ModConfigSpec.BooleanValue RELUCTANT_WAKE;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue DROP_ANIMATION;
    public static final ModConfigSpec.BooleanValue PICKUP_ANIMATION;
    public static final ModConfigSpec.BooleanValue BED_ANIMATION;
    public static final ModConfigSpec.BooleanValue REDUCED_MOTION;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_COMPATIBILITY;
    public static final ModConfigSpec.BooleanValue SHARE_ACTIVITY;
    public static final ModConfigSpec.BooleanValue SHARE_TYPING;
    public static final ModConfigSpec.BooleanValue SHARE_COMMANDS;
    public static final ModConfigSpec.BooleanValue SHARE_BROWSING;
    public static final ModConfigSpec.BooleanValue SHARE_IDLE;
    public static final ModConfigSpec.BooleanValue VIEW_ACTIVITY;
    public static final ModConfigSpec.BooleanValue BUBBLES;
    public static final ModConfigSpec.BooleanValue BROWSING_INDICATOR;
    public static final ModConfigSpec.BooleanValue IDLE_INDICATOR;
    public static final ModConfigSpec.BooleanValue ACTIVITY_TAB;
    public static final ModConfigSpec.BooleanValue ACTIVITY_PANEL;
    public static final ModConfigSpec.BooleanValue TYPING_GESTURE;
    public static final ModConfigSpec.BooleanValue BROWSING_GESTURE;
    public static final ModConfigSpec.BooleanValue IDLE_GESTURE;
    public static final ModConfigSpec.BooleanValue SHARE_INVENTORY_PREVIEW,BOOK_PREVIEWS,SOPHISTICATED_PREVIEWS,TRAVELER_PREVIEWS;
    public static final ModConfigSpec.BooleanValue VIEW_INVENTORY_PREVIEW;
    public static final ModConfigSpec.IntValue IDLE_SECONDS;
    public static final ModConfigSpec.IntValue ACTIVITY_DISTANCE;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.translation("rpg_animation.configuration.compatibility").push("compatibility");
        ADDITIONAL_ANIMATION_OWNERS=builder.comment("Optional mod IDs that should own all player poses and first-person presentation when installed. Leave empty for selective per-action control. This does not inspect those mods' internal enabled state.")
                .translation("rpg_animation.configuration.compatibility.additionalOwners").defineListAllowEmpty("additionalAnimationOwners",java.util.List.of(),()->"",v->v instanceof String id&&id.matches("[a-z][a-z0-9_]{1,63}"));
        PUNCHY_HANDOFF=builder.comment("Enable Punchy choices in per-action priority. Automatic uses a documented overlap profile for first person; it cannot inspect internal settings or resource-pack mappings. Third-person RPG poses remain available.")
                .translation("rpg_animation.configuration.compatibility.punchy").define("punchyHandoff",true);
        BETTER_COMBAT_HANDOFF=builder.comment("Enable Better Combat choices in per-action priority. Automatic yields overlapping combat actions during attacks; unrelated RPG actions remain enabled.")
                .translation("rpg_animation.configuration.compatibility.betterCombat").define("betterCombatHandoff",true);
        SAROS_HANDOFF=builder.comment("Yield player presentation while Saros owns that player as an active ragdoll. Does not alter physics or revive behaviour.").translation("rpg_animation.configuration.compatibility.saros").define("sarosHandoff",true);
        RAGDOLLIFIED_HANDOFF=builder.comment("Yield player presentation while Ragdollified hides that player. Corpses belonging to an old player entity do not suppress the respawned player.").translation("rpg_animation.configuration.compatibility.ragdollified").define("ragdollifiedHandoff",true);
        CARRY_ON_HANDOFF=builder.comment("Yield RPG player poses while Carry On reports a carried block or entity. Does not change carrying mechanics.")
                .translation("rpg_animation.configuration.compatibility.carryOn").define("carryOnHandoff",true);
        CORPSE_HANDOFF=builder.comment("Keep Corpse's dummy player free of live-player gestures and inventory panels; respawned players remain animated.")
                .translation("rpg_animation.configuration.compatibility.corpse").define("corpseHandoff",true);
        builder.pop();
        builder.translation("rpg_animation.configuration.actionOwners").push("actionPriority");
        for(String action:dev.shtark.rpganimation.animation.AnimationChannels.keys()) {
            ACTION_OWNERS.put(action,builder.comment("AUTO: documented overlap profile and live Better Combat attack state. RPG: prefer ours. PUNCHY: yield first person when installed. BETTER_COMBAT: yield both views when installed. Missing selected mods fall back to RPG; compatibility master switches still apply.")
                    .translation("rpg_animation.configuration.owner."+action).defineEnum(action,dev.shtark.rpganimation.animation.AnimationPreference.AUTO));
        }
        builder.pop();
        builder.translation("rpg_animation.configuration.basic").push("basicActions");
        for(var action:dev.shtark.rpganimation.animation.BasicAction.values()) {
            String group=switch(action) {
                case LAND_SOFT,LAND_HARD,CLIMB,SWIM_PUSH,SWIM_LOOP,SWIM_EXIT,DISMOUNT_HORSE,DISMOUNT_CAMEL,DISMOUNT_SMALL -> "movement";
                case SHIELD,SHIELD_HIT,BOW_DRAW,BOW_RELEASE,CROSSBOW_LOAD,CROSSBOW_AIM,CROSSBOW_RELEASE -> "equipment";
                case BUTTON,LEVER,PAGE,PLACE -> "interactions";case SLEEP_SETTLE -> "rest";default -> null;
            };
            if(group!=null)builder.translation("rpg_animation.configuration.basic."+group).push(group);
            String key=action.name().toLowerCase(java.util.Locale.ROOT);
            BASIC.put(action,builder.comment("Display cosmetic "+key+" motions. Gameplay is unchanged.").translation("rpg_animation.configuration.basic."+key).define(key,true));
            if(group!=null)builder.pop();
        }
        WALKING=builder.translation("rpg_animation.configuration.basic.walking").comment("Footfall-driven shoulder motion while walking on the ground.").define("walking",true);
        RUNNING=builder.translation("rpg_animation.configuration.basic.running").comment("Stronger opposing arms and forward lean while sprinting.").define("running",true);
        EATING=builder.translation("rpg_animation.configuration.basic.eating").comment("Bite rhythm for either hand. Consumption timing remains vanilla.").define("eating",true);
        builder.pop();
        builder.push("contextAnimations");
        for(var clip:dev.shtark.rpganimation.animation.ContextClip.values())if(!clip.detail())
            CONTEXT.put(clip,builder.comment("Enable cosmetic "+clip.name().toLowerCase(java.util.Locale.ROOT)+" gestures on this client.")
                    .translation("rpg_animation.configuration.context."+clip.name().toLowerCase(java.util.Locale.ROOT))
                    .define(clip.name().toLowerCase(java.util.Locale.ROOT),true));
        builder.pop();
        builder.translation("rpg_animation.configuration.details").push("rpgDetails");
        for(String group:java.util.List.of("environment","equipment","workstations","seats","social")) {
            builder.translation("rpg_animation.configuration.details."+group).push(group);
            for(var clip:dev.shtark.rpganimation.animation.ContextClip.values())if(clip.detail()&&clip.detailGroup().equals(group))
                CONTEXT.put(clip,builder.comment("Display this cosmetic gesture. Server settings and per-action ownership also apply; gameplay is unchanged.")
                        .translation("rpg_animation.configuration.context."+clip.name().toLowerCase(java.util.Locale.ROOT))
                        .define(clip.name().toLowerCase(java.util.Locale.ROOT),true));
            builder.pop();
        }
        builder.pop();
        RELUCTANT_WAKE=builder.comment("Show occasional seated face-rub variation when leaving bed.")
                .translation("rpg_animation.configuration.reluctantWake").define("reluctantWake",true);
        PICKUP_ANIMATION = builder.comment("Animate successful ground-item pickups; movement and inventory rules stay unchanged.")
                .translation("rpg_animation.configuration.pickupAnimation").define("pickupAnimation", true);
        DROP_ANIMATION = builder.comment("Animate successful item drops locally and on visible players; item rules are unchanged.")
                .translation("rpg_animation.configuration.dropAnimation").define("dropAnimation", true);
        BED_ANIMATION = builder.comment("Animate bed entry and exit for your camera and visible players. Does not change sleeping rules.")
                .translation("rpg_animation.configuration.bedAnimation").define("bedAnimation", true);
        ENABLED = builder.comment("Enable the wake-up intro. Set false to disable both first-entry playback and previews.",
                        "Disabling during playback ends it. Skipped first-entry intros remain marked as seen.")
                .translation("rpg_animation.configuration.enabled").define("enabled", true);
        REDUCED_MOTION = builder.comment("Keep normal eye height and use a single fade without camera movement or repeated blinks.")
                .translation("rpg_animation.configuration.reducedMotion").define("reducedMotion", false);
        FIRST_PERSON_COMPATIBILITY = builder.comment("Temporarily suppress First-person Model's body during the wake-up intro to avoid camera clipping.",
                        "Only applies when First-person Model is installed; its saved settings are never changed.")
                .translation("rpg_animation.configuration.firstPersonCompatibility").define("firstPersonCompatibility", true);
        builder.translation("rpg_animation.configuration.activity").push("activity");
        builder.translation("rpg_animation.configuration.activity.sharing").push("sharing");
        SHARE_ACTIVITY=builder.comment("Allow other players to see your activity status. Inventory images require the separate explicit preview opt-in.").translation("rpg_animation.configuration.activity.sharing.enabled").define("enabled",true);
        SHARE_TYPING=builder.comment("Share when ordinary chat is open and your coarse typing speed.").translation("rpg_animation.configuration.activity.sharing.typing").define("typing",true);
        SHARE_COMMANDS=builder.comment("Also show typing while entering slash commands. Command text is never sent.").translation("rpg_animation.configuration.activity.sharing.commands").define("commands",false);
        SOPHISTICATED_PREVIEWS=builder.comment("Include the Sophisticated backpack window and upgrade panels when inventory image sharing is enabled.").translation("rpg_animation.configuration.activity.sharing.sophisticated").define("sophisticatedBackpackPreviews",true);
        TRAVELER_PREVIEWS=builder.comment("Include Traveler backpack windows and visible side widgets when inventory image sharing is enabled.").translation("rpg_animation.configuration.activity.sharing.traveler").define("travelersBackpackPreviews",true);
        BOOK_PREVIEWS=builder.comment("Include visible book pages in opt-in preview sharing; may include text you write. Does not share signing screens or chat.").translation("rpg_animation.configuration.activity.sharing.books").define("bookPreviews",true);
        SHARE_INVENTORY_PREVIEW=builder.comment("Opt in to sharing a low-resolution image of your inventory window, including visible items and resource-pack appearance, with nearby players. Never captures chat or the desktop.").translation("rpg_animation.configuration.activity.sharing.preview").define("inventoryPreview",false);
        SHARE_BROWSING=builder.comment("Share inventory/container activity and a coarse click pulse, never contents.").translation("rpg_animation.configuration.activity.sharing.browsing").define("browsing",true);
        SHARE_IDLE=builder.comment("Share an idle status after the configured time without input.").translation("rpg_animation.configuration.activity.sharing.idle").define("idle",true);
        builder.pop();
        builder.translation("rpg_animation.configuration.activity.display").push("display");
        VIEW_ACTIVITY=builder.comment("Master switch for activity indicators, panels and gestures on this client.").translation("rpg_animation.configuration.activity.display.enabled").define("enabled",true);
        BUBBLES=builder.comment("Show an animated typing indicator above visible players.").translation("rpg_animation.configuration.activity.display.typingBubbles").define("typingBubbles",true);
        BROWSING_INDICATOR=builder.comment("Show a browsing indicator above visible players.").translation("rpg_animation.configuration.activity.display.inventoryIndicator").define("inventoryIndicator",true);
        IDLE_INDICATOR=builder.comment("Show Zzz above idle players.").translation("rpg_animation.configuration.activity.display.idleIndicator").define("idleIndicator",true);
        ACTIVITY_TAB=builder.comment("Show activity beside player names in the Tab list.").translation("rpg_animation.configuration.activity.display.tabStatus").define("tabStatus",true);
        VIEW_INVENTORY_PREVIEW=builder.comment("Show inventory-window previews from players who opt in. Does not enable sharing your own screen.").translation("rpg_animation.configuration.activity.display.preview").define("inventoryPreview",true);
        ACTIVITY_PANEL=builder.comment("Show the floating panel for inventory previews shared by other players.").translation("rpg_animation.configuration.activity.display.floatingPanel").define("floatingPanel",true);
        builder.pop();
        builder.translation("rpg_animation.configuration.activity.gestures").push("gestures");
        TYPING_GESTURE=builder.comment("Continuous alternating hand movements while typing, easing out after a pause.").translation("rpg_animation.configuration.activity.gestures.typing").define("typing",true);
        BROWSING_GESTURE=builder.comment("Look down and reach toward the generic panel when clicking.").translation("rpg_animation.configuration.activity.gestures.browsing").define("browsing",true);
        IDLE_GESTURE=builder.comment("Relax shoulders and nod the head slowly while idle.").translation("rpg_animation.configuration.activity.gestures.idle").define("idle",true);
        builder.pop();
        builder.translation("rpg_animation.configuration.activity.timing").push("timing");
        IDLE_SECONDS=builder.comment("Seconds without keyboard, mouse or avatar activity before idle. Applies live.").translation("rpg_animation.configuration.activity.idleSeconds").defineInRange("idleSeconds",300,30,3600);
        ACTIVITY_DISTANCE=builder.comment("Maximum distance in blocks for world indicators and panels. Tab status is independent.").translation("rpg_animation.configuration.activity.distance").defineInRange("distance",24,4,64);
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }
    private ClientConfig() {}
}
