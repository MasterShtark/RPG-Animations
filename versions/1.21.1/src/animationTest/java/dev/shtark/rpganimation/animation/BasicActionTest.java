package dev.shtark.rpganimation.animation;

public final class BasicActionTest {
    public static void run() {
        check(MountCompatibility.mounting("minecraft:horse",false)==BasicAction.HORSE,"Vanilla mount retained");
        check(MountCompatibility.dismounting("minecraft:camel",false)==BasicAction.DISMOUNT_CAMEL,"Vanilla tall dismount retained");
        check(MountCompatibility.dismounting("minecraft:pig",false)==BasicAction.DISMOUNT_SMALL,"Vanilla small dismount retained");
        for(String id:new String[]{"othermod:horse","example:large_creature","example:small_creature"}) {
            check(MountCompatibility.mounting(id,false)==null && MountCompatibility.dismounting(id,false)==null,"Unknown mount yields both transitions");
            check(MountCompatibility.mounting(id,true)==BasicAction.OTHER_MOUNT,"Explicit generic opt-in, no vanilla namespace collision");
            check(MountCompatibility.dismounting(id,true)==BasicAction.DISMOUNT_HORSE,"Explicit generic dismount opt-in");
        }
        for(var action:BasicAction.values()) {
            var first=values(action.sample(-1));var last=values(action.sample(2));
            for(int i=0;i<first.length;i++)check(first[i]==0&&last[i]==0,"Neutral/clamped endpoints "+action);
            double[] previous=first;
            for(int step=0;step<=1000;step++) {
                var current=values(action.sample(step/1000.0));
                for(int i=0;i<current.length;i++) {
                    check(Double.isFinite(current[i])&&Math.abs(current[i])<3,"Finite rigid-limb bounds "+action);
                    check(Math.abs(current[i]-previous[i])<.08,"No pose jumps "+action);
                }
                check(current[7]>=0&&current[7]<=1,"Blend envelope "+action);
                previous=current;
            }
            for(double endpoint:new double[]{.0001,.9999})
                for(double value:values(action.sample(endpoint)))check(Math.abs(value)<.00001,"Settled endpoint velocity "+action);
        }
        check(BasicAction.LAND_HARD.sample(.18).lean()>BasicAction.LAND_SOFT.sample(.18).lean()*2,"Hard landing has deeper impact");
        check(BasicAction.LAND_HARD.sample(.3).leg()!=BasicAction.LAND_HARD.sample(.3).otherLeg(),"Hard landing uses staggered legs");
        check(BasicAction.LAND_HARD.duration==26,"Previous landing timing retained");
        check(BasicAction.CHOP.sample(.2).arm()!=BasicAction.DIG.sample(.2).arm(),"Chop and shovel differ");
        check(BasicAction.CHEST.sample(.42).support()<-.8,"Chest uses assisting hand");
        check(BasicAction.CAMEL.sample(.3).leg()<BasicAction.PIG.sample(.3).leg(),"Tall and small mounts differ");
        check(BasicAction.HORSE.sample(.3).leg()<BasicAction.HORSE.sample(.3).otherLeg(),"Leading leg before trailing leg");
        System.out.println("Passed: basic action bounds, continuity, endpoint settling, distinct tools/containers and sequential mount legs");
    }
    private static double[] values(BasicAction.Pose p){return new double[]{p.arm(),p.yaw(),p.roll(),p.support(),p.lean(),p.leg(),p.otherLeg(),p.weight()};}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
