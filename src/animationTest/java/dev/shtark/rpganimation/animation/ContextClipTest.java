package dev.shtark.rpganimation.animation;

public final class ContextClipTest {
    public static void run() {
        for(int i=0;i<=100;i++) {
            double angle=i*.006;
            var shoulder=HipBend.rotate(-12,0,angle);
            var torsoBottom=HipBend.rotate(12,0,angle);
            check(Math.abs(shoulder.y()+torsoBottom.y())<1e-9 && Math.abs(shoulder.z()+torsoBottom.z())<1e-9,"Bow torso bottom stays at hip pivot");
            var arm=HipBend.rotate(-10,0,angle);
            check(Math.abs(Math.hypot(arm.y()-shoulder.y(),arm.z()-shoulder.z())-2)<1e-9,"Bow shoulder/arm attachment remains rigid");
        }
        check(!ContextPolicy.mayStart(false,100,0,null,ContextClip.FLINCH),"Gameplay interruption overrides priority");
        check(!ContextPolicy.mayStart(true,99,100,null,ContextClip.CATCH),"Cooldown rejects early request");
        check(ContextPolicy.mayStart(true,100,100,null,ContextClip.CATCH),"Cooldown exact boundary");
        check(!ContextPolicy.mayStart(true,100,0,ContextClip.CATCH,ContextClip.CATCH),"Burst cannot restart clip");
        check(!ContextPolicy.mayStart(true,100,0,ContextClip.FLINCH,ContextClip.WARM),"Ambient cannot replace reaction");
        check(ContextPolicy.mayStart(true,100,0,ContextClip.WARM,ContextClip.FLINCH),"Reaction preempts ambient");
        check(!ContextClip.NOD.usesSupportHand()&&!ContextClip.WAVE.usesSupportHand(),"Single-arm gestures do not summon an empty offhand");
        check(ContextClip.ADJUST_HELMET.usesSupportHand(),"Two-handed adjustment retains assisting hand");
        check(ContextClip.INSPECT_SPECIAL_SWORD.duration>ContextClip.INSPECT_SWORD.duration,"Special sword has a longer study/test sequence");
        check(ContextClip.INSPECT_TREASURE.usesSupportHand()&&ContextClip.INSPECT_SPECIAL_GEAR.usesSupportHand(),"Rare presentations use both hands");
        check(!ContextClip.INSPECT_AXE.usesSupportHand()&&!ContextClip.INSPECT_PICKAXE.usesSupportHand(),"Ordinary tool inspection leaves offhand alone");
        for(var clip:ContextClip.values())if(clip.inspection()) {
            String key=clip.name().toLowerCase(java.util.Locale.ROOT);
            check(AnimationChannels.inspectionAction(key)&&AnimationChannels.detailAction(key),"Inspection yields during active external combat");
        }
        check(!ContextClip.INSPECT.twoHandInspection()&&!ContextClip.INSPECT_SPECIAL_SWORD.twoHandInspection(),"Incidental support motion cannot select the two-arm item renderer");
        check(ContextClip.INSPECT_GEAR.twoHandInspection()&&ContextClip.INSPECT_TREASURE.twoHandInspection(),"Presentation clips select both arms");
        var history=new DetailActivity();
        for(int tick=0;tick<59;tick++)history.tick(tick,true,true,true,false);
        history.tick(59,false,false,true,false);check(history.breath(59)==-1,"Brief sprint has no recovery");
        for(int tick=60;tick<360;tick++)history.tick(tick,true,true,true,false);
        history.tick(360,false,false,true,false);check(history.breath(360)==3,"Long sprint produces stronger recovery");
        check(history.breath(421)==-1,"Old recovery expires instead of surprising later playback");
        for(int tick=500;tick<520;tick++)history.tick(tick,false,false,false,true);
        history.tick(520,false,false,true,false);check(history.wipe(520),"Sustained submersion offers surface wipe");
        history.wiped();check(!history.wipe(521),"Surface wipe consumed once");
        for(boolean entering:new boolean[]{true,false}) {
            double last=DetailActivity.seatLeg(entering,0,30);
            check(Math.abs(last)==1.35,"Seat starts at previous posture");
            for(double t=.01;t<=30;t+=.01){double current=DetailActivity.seatLeg(entering,t,30);
                check(Math.abs(current-last)<.003,"Seat leg transfer stays continuous");last=current;}
            check(DetailActivity.seatLeg(entering,30,30)==0&&DetailActivity.seatTravel(30,30)==0,"Seat reaches native endpoint");
        }
        for(var clip:ContextClip.values())for(int variant=0;variant<4;variant++) {
            var start=clip.sample(0,variant);var end=clip.sample(clip.duration,variant);
            check(start.equals(end)&&start.weight()==0,"Neutral endpoints "+clip);
            check(clip.sample(-10,variant).equals(start)&&clip.sample(1000,variant).equals(end),"Clamp "+clip);
            double[] previous=values(start);double energy=0;
            for(double time=.02;time<=clip.duration;time+=.02) {
                double[] current=values(clip.sample(time,variant));
                for(int i=0;i<current.length;i++) {
                    check(Double.isFinite(current[i])&&Math.abs(current[i])<=2.6,"Finite bounded pose "+clip);
                    check(Math.abs(current[i]-previous[i])<.15,"No pose jumps "+clip);
                    energy+=Math.abs(current[i]);
                }
                check(current[9]>=0&&current[9]<=1,"Weight bounds "+clip+" at "+time+": "+current[9]);previous=current;
            }
            check(energy>10,"Distinct active motion "+clip);
            check(clip.cooldown>clip.duration,"No queued looping "+clip);
            var late=new PlaybackSnapshot(5,100);
            check(clip.sample(late.time(103,.5),variant).equals(clip.sample(8.5,variant)),"Late phase "+clip);
        }
        for(double distance=.3;distance<=.7;distance+=.01) {
            double angle=ContextClip.wallRoll(distance,1);
            double palm=5*.9375/16-10*.9375/16*Math.sin(angle);
            check(Math.abs(palm-distance)<1e-8,"Wall plane contact at full support");
        }
        check(BedExitSequence.reluctantPhase(0)==88&&BedExitSequence.reluctantPhase(86)==0,"Reluctant endpoints");
        check(BedExitSequence.reluctantPhase(12)==68&&BedExitSequence.reluctantPhase(27)==68,"Seated face-rub hold");
        check(BedExitSequence.faceRub(12)==0&&BedExitSequence.faceRub(18)==1&&BedExitSequence.faceRub(28)==0,"Face rub envelope");
        check(Math.abs(BedExitSequence.reluctantPhase(11.999)-68)<.00001
                &&Math.abs(BedExitSequence.reluctantPhase(28.001)-68)<.00001,"Smooth stop and departure at seated pause");
        double last=88;
        for(double time=.01;time<=86;time+=.01) {
            double phase=BedExitSequence.reluctantPhase(time);
            check(phase<=last+1e-8&&last-phase<.1,"Reluctant continuous forward progression");last=phase;
        }
        System.out.println("Passed: all contextual clips/four variation codes, activity history, seat transfer, bounds, neutral endpoints, continuity, late phase, wall-plane solve and reluctant exit");
    }
    private static double[] values(ContextClip.Pose p){return new double[]{p.mainX(),p.mainY(),p.mainZ(),p.otherX(),p.otherY(),p.otherZ(),p.head(),p.lean(),p.roll(),p.weight()};}
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
}

