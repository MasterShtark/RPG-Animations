package dev.shtark.rpganimation.animation;

/** Full registry IDs prevent foreign mounts from accidentally receiving vanilla profiles. */
public final class MountCompatibility {
    private MountCompatibility() {}
    public static BasicAction mounting(String id, boolean allowUnknown) {
        return switch(id) {
            case "minecraft:horse","minecraft:skeleton_horse","minecraft:zombie_horse" -> BasicAction.HORSE;
            case "minecraft:donkey","minecraft:mule" -> BasicAction.DONKEY;
            case "minecraft:camel" -> BasicAction.CAMEL;
            case "minecraft:llama","minecraft:trader_llama" -> BasicAction.LLAMA;
            case "minecraft:pig" -> BasicAction.PIG;
            case "minecraft:strider" -> BasicAction.STRIDER;
            default -> allowUnknown ? BasicAction.OTHER_MOUNT : null;
        };
    }
    public static BasicAction dismounting(String id, boolean allowUnknown) {
        BasicAction mount=mounting(id,allowUnknown);
        if(mount==null)return null;
        return switch(mount) {
            case CAMEL -> BasicAction.DISMOUNT_CAMEL;
            case PIG,STRIDER -> BasicAction.DISMOUNT_SMALL;
            default -> BasicAction.DISMOUNT_HORSE;
        };
    }
}
