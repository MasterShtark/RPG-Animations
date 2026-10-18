package dev.shtark.rpganimation.animation;
public final class AnimationOwnershipTest {
    private static boolean allows(AnimationPreference p,boolean first,boolean punchy,boolean covered,boolean combat,boolean attack,boolean unknown,boolean combatAction){return AnimationOwnership.allowed(p,first,punchy,covered,combat,attack,unknown,combatAction);}
    public static void run(){
        for(var preference:AnimationPreference.values())for(int bits=0;bits<16;bits++) {
            boolean first=(bits&1)!=0,covered=(bits&2)!=0,active=(bits&4)!=0,unknown=(bits&8)!=0;
            check(allows(preference,first,false,covered,false,active,unknown,true),"Absent mods always fall back to RPG");
            check(allows(AnimationPreference.RPG,first,true,covered,true,active,unknown,true),"Explicit RPG wins");
            check(allows(AnimationPreference.PUNCHY,false,true,covered,false,active,unknown,false),"Punchy does not provide third-person body");
        }
        check(!allows(AnimationPreference.AUTO,true,true,true,false,false,false,false),"Known Punchy first-person overlap yields");
        check(allows(AnimationPreference.AUTO,true,true,false,false,false,false,false),"RPG-only action survives Punchy presence");
        check(allows(AnimationPreference.AUTO,true,false,false,true,false,false,true),"Idle Better Combat weapon is not an active attack");
        check(allows(AnimationPreference.AUTO,false,false,false,true,true,false,false),"Active combat preserves unrelated cosmetic actions");
        check(!allows(AnimationPreference.AUTO,false,false,false,true,true,false,true),"Active combat owns overlapping combat actions");
        check(allows(AnimationPreference.AUTO,false,false,false,true,false,true,false),"Unknown combat API preserves noncombat actions");
        check(!allows(AnimationPreference.AUTO,false,false,false,true,false,true,true),"Unknown combat API yields combat actions only");
        check(!allows(AnimationPreference.BETTER_COMBAT,false,false,false,true,false,false,false),"Explicit external ownership is honoured");
        var keys=AnimationChannels.keys();check(new java.util.HashSet<>(keys).size()==keys.size(),"Unique action config IDs");
        check(keys.containsAll(AnimationChannels.PUNCHY),"Every documented profile action is configurable");
        for(String key:java.util.List.of("wake_up","bed_entry","bed_exit","pickup","drop","horse","page","sleep_settle"))
            check(!AnimationChannels.PUNCHY.contains(key),"Do not invent Punchy coverage for "+key);
        for(String key:keys) for(boolean first:new boolean[]{false,true}) {
            boolean punchyCovers=AnimationChannels.PUNCHY.contains(key);
            boolean combatCovers=AnimationChannels.combatAction(key);
            if(!punchyCovers&&!combatCovers) {
                check(allows(AnimationPreference.AUTO,first,true,false,true,true,false,false),"Both installed, active combat preserves "+key);
                check(allows(AnimationPreference.AUTO,first,true,false,true,false,true,false),"Unknown combat API preserves "+key);
            }
        }
        System.out.println("Passed: per-action fallback, absent mods, explicit ownership, third-person scope, active/idle combat and profile coverage");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
