package dev.shtark.rpganimation.animation;

import java.util.UUID;

public final class ActivityTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void run() {
        check(ActivityState.handHandoff(0)==0 && ActivityState.handHandoff(.2)==1,"Vanilla hands fully lowered at render-path switch");
        check(ActivityState.typingLift(.2)==0 && ActivityState.typingLift(1)==1,"Custom hands enter from lowered endpoint");
        double previousH=0,previousL=0;
        for(int i=0;i<=1000;i++) {
            double w=i/1000.0,h=ActivityState.handHandoff(w),l=ActivityState.typingLift(w);
            check(h>=previousH && l>=previousL && h-previousH<.008 && l-previousL<.002,"Bounded continuous hand transfer in either direction");
            previousH=h;previousL=l;
        }
        check(InventoryPreview.valid(-1,0,0),"Tiny unchanged-image lease accepted");
        check(!InventoryPreview.valid(-1,0,1)&&!InventoryPreview.valid(-1,1,0),"Lease cannot carry hidden image bytes");
        check(!InventoryPreview.valid(-2,0,0)&&!InventoryPreview.valid(128,96,-1),"Invalid sentinels and lengths rejected");
        var budget=new ByteBudget();
        check(budget.take(0,24576,32768),"First image fits aggregate budget");
        check(!budget.take(0,24576,32768),"Second image exceeds shared window budget");
        check(budget.take(0,8192,32768)&&!budget.take(0,1,32768),"Exact byte ceiling");
        check(budget.take(1,24576,32768),"New window replenishes budget");
        check(!budget.take(1,-1,32768)&&!budget.take(1,Integer.MAX_VALUE,32768),"Malformed/oversized cost cannot bypass budget");
        check(!budget.take(0,1,32768),"Delayed decoder from an older window cannot replenish the current budget");
        check(budget.take(1,8192,32768)&&!budget.take(1,1,32768),"Out-of-order request preserves remaining current-window allowance");
        budget.reset();check(budget.take(0,32768,32768),"New server session resets its tick-clock budget");
        var changed=new PreviewChanges();byte[] pixels={1,2,3,4};
        check(changed.update(2,1,pixels),"Initial image transmitted");int revision=changed.revision();
        check(!changed.update(2,1,pixels.clone())&&changed.revision()==revision,"Identical image produces lease without changing revision");
        pixels[0]=5;check(changed.update(2,1,pixels)&&changed.revision()!=revision,"Pixel change transmitted; source arrays cannot mutate cached snapshot");
        check(changed.update(1,2,pixels),"Dimension change with same bytes transmitted");
        changed.invalidate();check(changed.update(1,2,pixels),"Server refresh/opt-out forces full image recovery");
        var id=UUID.randomUUID();var otherId=UUID.randomUUID();
        var current=java.util.Map.of(id,new ActivityDelta.Value(1,2));
        check(ActivityDelta.between(current,current).isEmpty(),"Unchanged heartbeat sends zero status changes");
        var desired=java.util.Map.of(id,new ActivityDelta.Value(1,3),otherId,new ActivityDelta.Value(3,0));
        check(ActivityDelta.between(current,desired).size()==2,"Typing intensity change and new subscriber snapshot retained");
        check(ActivityDelta.between(current,current,true).size()==1,"Dimension/subscription refresh resends unchanged status to a reset client");
        var cleared=ActivityDelta.between(current,java.util.Map.of());
        check(cleared.get(id).kind()==0,"Opt-out, range exit and expiry produce explicit removal");
        check(!InventoryPreview.valid(10000,10000,0),"Oversize preview rejected");
        check(!InventoryPreview.valid(128,96,24575),"Truncated preview rejected");
        check(InventoryPreview.valid(128,96,24576)&&InventoryPreview.valid(0,0,0),"Bounded image and clear accepted");
        for(int color:new int[]{0xff0000ff,0xff00ff00,0xffff0000,0xffffffff,0xff000000})
            check(InventoryPreview.decode(InventoryPreview.encode(color))==color,"Primary color roundtrip preserves ABGR");
        var ledger=new ActivityLedger();var a=UUID.randomUUID();var b=UUID.randomUUID();
        check(!ledger.update(a,255,3,0),"Unknown status rejected");
        check(!ledger.update(a,1,255,0),"Malformed intensity rejected");
        check(!ledger.update(a,3,1,0),"Idle cannot carry input intensity");
        check(ledger.update(a,1,2,0),"Typing accepted");
        check(!ledger.update(a,2,3,1),"Burst rejected");
        check(ledger.update(b,2,3,1),"Independent player is not throttled");
        check(ledger.snapshot().get(a).kind()==1,"Rejected request leaves original state");
        check(ledger.update(a,2,3,5),"Valid transition accepted at boundary");
        var snapshot=ledger.snapshot();
        check(!ledger.expired(a,104)&&ledger.expired(a,105),"Missing heartbeat expires exactly at TTL");
        check(ledger.update(a,0,0,10),"Opt-out clears active state");
        check(!ledger.snapshot().containsKey(a)&&snapshot.containsKey(a),"Snapshot immutable across updates");
        ledger.clear(b);check(ledger.update(b,3,0,2),"Reconnect resets rate limiter");
        ledger.reset();check(ledger.snapshot().isEmpty(),"Server shutdown clears state");
        for(int k=0;k<=3;k++)for(int i=0;i<4;i++) {
            var previous=ActivityState.pose(k,i,0);
            for(double t=.1;t<600;t+=.1) {
                var p=ActivityState.pose(k,i,t);
                check(Double.isFinite(p.right()+p.left()+p.head()+p.roll()),"Finite pose");
                check(Math.abs(p.right())<1.4&&Math.abs(p.left())<1.4&&Math.abs(p.head())<.9,"Bounded rig");
                check(Math.abs(p.right()-previous.right())<.02,"Continuous loop");previous=p;
            }
        }
        for(int quiet=0;quiet<30;quiet++)check(ActivityState.typingIntensity(quiet)==2,"Inter-key gaps preserve typing loop");
        check(ActivityState.typingIntensity(30)==0,"Sustained pause settles hands");
        for(double time:new double[]{7,83,127,399}) {
            double low=ActivityState.pose(ActivityState.TYPING,1,time).right()+.72;
            double high=ActivityState.pose(ActivityState.TYPING,3,time).right()+.72;
            check(Math.abs(high-3*low)<1e-12,"Input intensity cannot reset oscillator phase");
        }
        check(ActivityState.blend(0)==0&&ActivityState.blend(10)==1,"Exact transition endpoints");
        check(ActivityState.blend(.01)<.00001&&1-ActivityState.blend(9.99)<.00001,"Soft transition endpoints");
        System.out.println("Passed: byte ceilings, preview leases/content/recovery, activity deltas, activity rejection, per-sender throttling, opt-out, expiry, snapshots, cleanup and continuous bounded poses");
    }
}
