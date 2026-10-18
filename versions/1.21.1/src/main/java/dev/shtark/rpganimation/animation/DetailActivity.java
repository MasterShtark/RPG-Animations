package dev.shtark.rpganimation.animation;

/** Per-session, bounded activity history. No entity, level or item references. */
public final class DetailActivity {
    private int sprint,submerged,breathUntil=-1,wetUntil=-1,strength;
    public void tick(int now,boolean sprinting,boolean moving,boolean grounded,boolean underwater) {
        if(sprinting&&moving&&grounded)sprint=Math.min(400,sprint+1);
        else if(!sprinting&&sprint>0){
            if(sprint>=60){strength=Math.min(3,sprint/100);breathUntil=now+60;}
            sprint=0;
        }
        if(underwater)submerged=Math.min(100,submerged+1);
        else if(submerged>0){if(submerged>=20)wetUntil=now+100;submerged=0;}
    }
    public int breath(int now){return breathUntil>=now?strength:-1;}
    public boolean wipe(int now){return wetUntil>=now;}
    public void breathed(){breathUntil=-1;}
    public void wiped(){wetUntil=-1;}
    public static double seatLeg(boolean entering,double ticks,double duration){
        return (entering?1.35:-1.35)*(1-MotionCurve.ease(ticks/(duration*.8)));
    }
    public static double seatTravel(double ticks,double duration){return 1-MotionCurve.ease(ticks/(duration*.85));}
}
