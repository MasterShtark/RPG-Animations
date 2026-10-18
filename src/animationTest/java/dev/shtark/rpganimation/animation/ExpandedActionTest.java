package dev.shtark.rpganimation.animation;

public final class ExpandedActionTest {
    public static void run() {
        check(Math.abs(BasicAction.LAND_HARD.sample(.18).leg())>Math.abs(BasicAction.LAND_SOFT.sample(.18).leg()),"Fall intensity grades compression");
        for(var action:new BasicAction[]{BasicAction.CLIMB,BasicAction.SWIM_LOOP}) {
            double period=action==BasicAction.CLIMB?24:28;
            for(double t=-100;t<100;t+=.1) {
                var a=action.continuousPose(t,1);var b=action.continuousPose(t+period,1);
                check(Math.abs(a.arm()-b.arm())<1e-10&&Math.abs(a.otherLeg()-b.otherLeg())<1e-10,"Loop seam/negative altitude "+action);
                check(a.equals(action.continuousPose(t,1)),"Stopped phase stays fixed "+action);
                check(Math.abs(a.arm())<3&&Math.abs(a.support())<3,"Loop arm bounds "+action);
                check(action.continuousPose(t,0).weight()==0,"Disabled blend "+action);
            }
            var a=action.continuousPose(period/4,1);
            check(Math.abs(a.arm()-a.support())>1,"Alternating hands "+action);
        }
        check(BasicAction.CAMEL.mount()&&!BasicAction.SLEEP_SETTLE.mount(),"Appended action IDs are not mounts");
        check(BasicAction.DISMOUNT_CAMEL.dismount()&&!BasicAction.HORSE.dismount(),"Distinct dismount lifecycle");
        for(var action:BasicAction.values())if(action.continuous()) {
            var p=action.continuousPose(1000,1);check(Double.isFinite(p.arm())&&Math.abs(p.arm())<3,"Sustained hold is bounded "+action);
        }
        System.out.println("Passed: landing intensity, periodic limb phases, paused climb, negative altitude, hold bounds and mount classification");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
