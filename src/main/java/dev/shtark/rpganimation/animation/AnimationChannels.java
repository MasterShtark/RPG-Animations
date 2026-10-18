package dev.shtark.rpganimation.animation;
import java.util.*;
/** Documentation-based overlap profile, not introspection of external settings. */
public final class AnimationChannels {
    private AnimationChannels() {}
    public static final Set<String> PUNCHY=Set.of("swing","chop","dig","pick","till","walking","running","eating",
        "land_soft","land_hard","shield","shield_hit","bow_draw","bow_release","crossbow_load","crossbow_aim","crossbow_release",
        "elytra_takeoff","elytra_flight","elytra_land","fish_cast","fish_reel","fishing_hold","inspect");
    private static final Set<String> COMBAT=Set.of("swing","chop","dig","pick");
    public static boolean combatAction(String key){return COMBAT.contains(key);}
    private static final Set<String> DETAILS=java.util.Arrays.stream(ContextClip.values()).filter(ContextClip::detail)
            .map(c->c.name().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    public static boolean inspectionAction(String key){return key!=null&&(key.equals("inspect")||key.startsWith("inspect_"));}
    public static boolean detailAction(String key){return key!=null&&(DETAILS.contains(key)||inspectionAction(key));}
    public static List<String> keys(){
        var keys=new LinkedHashSet<String>();
        for(var action:BasicAction.values())keys.add(action.name().toLowerCase(Locale.ROOT));
        for(var action:ContextClip.values())keys.add(action.name().toLowerCase(Locale.ROOT));
        keys.addAll(List.of("wake_up","bed_entry","bed_exit","drop","pickup","walking","running","eating",
            "fishing_hold","activity_typing","activity_browsing","activity_idle","idle"));
        return List.copyOf(keys);
    }
}
