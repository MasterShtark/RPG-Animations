package dev.shtark.rpganimation.animation;

/** Only appearance/type changes count, never mutations of stored contents. */
public final class EquipTransition {
    private EquipTransition() {}
    public static boolean equipped(String before,String after){return before!=null&&!after.isEmpty()&&!after.equals(before);}
}
