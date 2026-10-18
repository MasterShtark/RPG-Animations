package dev.shtark.rpganimation.animation;
/** Presentation ownership only; never changes gameplay or weapon assignments. */
public final class AnimationOwnership {
    private AnimationOwnership() {}
    public static boolean allowed(AnimationPreference preference,boolean firstPerson,boolean punchy,boolean punchyCovers,
                                  boolean combat,boolean combatActive,boolean combatUnknown,boolean combatAction){
        return switch(preference){
            case RPG -> true;
            case PUNCHY -> !firstPerson||!punchy;
            case BETTER_COMBAT -> !combat;
            case AUTO -> !(combat&&combatAction&&(combatActive||combatUnknown))&&!(firstPerson&&punchy&&punchyCovers);
        };
    }
}
