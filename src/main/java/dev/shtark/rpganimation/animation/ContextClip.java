package dev.shtark.rpganimation.animation;

/** Original rigid-limb clips. Angles are radians; every channel has a continuous velocity curve. */
public enum ContextClip {
    CATCH(24,160,4), BRUSH(48,400,3), SHADE(38,240,1), BALANCE(22,100,5),
    WALL(24,100,1), SHAKE(34,200,2), POCKETS(36,120,1), PROTECT(28,120,2),
    INSPECT(48,200,3), WARM(56,400,1), FLINCH(16,60,6),
    FISH_CAST(24,25,4), FISH_WAIT(60,200,1), FISH_BITE(20,40,5), FISH_REEL(30,31,5),
    STRETCH_SMALL(44,600,0), STRETCH_MEDIUM(68,1200,0), STRETCH_LARGE(90,2400,0), FIRE_PAT(32,100,4), FREEZE_RUB(52,400,1), NETHER_SWEAT(44,1200,0), END_CHILL(56,1200,0),
    SPRINT_RECOVERY(80,180,1), LEDGE_LOOK(48,240,0), SURFACE_WIPE(46,150,2), RAIN_SHIELD(70,260,0), CAREFUL_CROUCH(50,90,0), INJURED_REST(80,200,1), ADJUST_HELMET(42,100,2), ADJUST_CHEST(44,100,2), ADJUST_LEGS(42,100,2), ADJUST_BOOTS(48,100,2), CRAFT(36,65,2), ANVIL_WORK(42,70,2), ENCHANT_WORK(54,80,2), BREW_WORK(44,70,2), SEAT_ENTER(30,45,2), SEAT_EXIT(32,45,2), WAVE(52,80,2), NOD(30,50,2), BOW(48,70,2), BECKON(48,70,2), POINT(52,80,2), THANKS(42,65,2), CAMPFIRE_REST(180,240,0),
    INSPECT_SWORD(64,200,3), INSPECT_AXE(60,200,3), INSPECT_PICKAXE(60,200,3), INSPECT_SHOVEL(56,200,3), INSPECT_HOE(56,200,3),
    INSPECT_SPECIAL_SWORD(110,240,3), INSPECT_SPECIAL_TOOL(84,220,3), INSPECT_GEAR(64,200,3), INSPECT_SPECIAL_GEAR(96,220,3), INSPECT_TREASURE(90,220,3);
    public final int duration, cooldown, priority;
    ContextClip(int duration,int cooldown,int priority) {
        this.duration=duration;this.cooldown=cooldown;this.priority=priority;
    }
    public record Pose(double mainX,double mainY,double mainZ,double otherX,double otherY,double otherZ,
                       double head,double lean,double roll,double weight) {}
    public static Pose blend(Pose a,Pose b,double progress) {
        double t=MotionCurve.ease(progress),u=1-t;
        return new Pose(a.mainX*u+b.mainX*t,a.mainY*u+b.mainY*t,a.mainZ*u+b.mainZ*t,
                a.otherX*u+b.otherX*t,a.otherY*u+b.otherY*t,a.otherZ*u+b.otherZ*t,
                a.head*u+b.head*t,a.lean*u+b.lean*t,a.roll*u+b.roll*t,a.weight*u+b.weight*t);
    }
    public boolean fishing(){return name().startsWith("FISH_");}
    public boolean environmental(){return this==FIRE_PAT||this==FREEZE_RUB||this==NETHER_SWEAT||this==END_CHILL;}
    public boolean stretch(){return name().startsWith("STRETCH_");}
    public boolean twoHandInspection(){return this==INSPECT_GEAR||this==INSPECT_SPECIAL_GEAR||this==INSPECT_TREASURE||this==INSPECT_SPECIAL_TOOL;}
    public boolean inspection(){return this==INSPECT||name().startsWith("INSPECT_");}
    public boolean detail(){return ordinal()>=SPRINT_RECOVERY.ordinal()&&!inspection();}
    public boolean social(){return this==WAVE||this==NOD||this==BOW||this==BECKON||this==POINT||this==THANKS;}
    public String detailGroup(){
        if(social()||this==CAMPFIRE_REST)return "social";
        if(name().startsWith("ADJUST_"))return "equipment";
        if(this==CRAFT||name().endsWith("_WORK"))return "workstations";
        if(seated())return "seats";
        return "environment";
    }
    public boolean seated(){return this==SEAT_ENTER||this==SEAT_EXIT;}
    private static double[] p(double a,double b,double c,double d,double e,double f,double h,double l,double r) {
        return new double[]{a,b,c,d,e,f,h,l,r};
    }
    private double[][] poses() {
        return switch(this) {
            default -> inspection()?InspectionPoses.keys(this):DetailPoses.keys(this);
            case FISH_CAST -> new double[][]{p(-1.8,-.2,-.15,-.3,0,.1,-.06,-.08,-.04),p(-.55,.12,-.1,-.6,.2,.15,.08,.12,.05),p(-.95,0,-.08,-.2,0,.05,.04,.04,.015),p(-.5,0,0,0,0,0,0,0,0)};
            case FISH_WAIT -> new double[][]{p(-.6,0,-.08,-.1,0,.03,.05,.015,0),p(-.72,.08,-.12,-.28,.12,.08,.08,.025,.015),p(-.65,-.06,-.08,-.15,0,.04,.04,.015,-.01),p(-.48,0,-.04,0,0,0,0,0,0)};
            case FISH_BITE -> new double[][]{p(-.65,0,-.1,-.25,0,.1,.1,.08,0),p(-1.4,-.12,-.2,-.65,.2,.12,-.06,-.08,-.04),p(-1.05,.05,-.12,-.35,.1,.08,.03,-.025,.015),p(-.6,0,-.05,-.1,0,0,0,0,0)};
            case FISH_REEL -> new double[][]{p(-1.6,-.15,-.25,-.7,.3,-.2,-.08,-.1,-.05),p(-1.25,.1,-.1,-1.15,.4,-.25,.06,-.04,.025),p(-.95,-.05,-.12,-.75,.15,-.1,.1,.03,0),p(-.5,0,-.03,-.15,0,0,.02,0,0)};
            case STRETCH_SMALL -> new double[][]{p(-.2,-.1,-.2,-.1,.1,.15,.05,0,.02),p(-.35,-.25,-.4,-.15,.1,.2,-.12,-.025,-.04),p(-.1,0,-.15,-.3,.2,.35,.08,0,.04),p(0,0,-.05,-.1,0,.1,0,0,0)};
            case STRETCH_MEDIUM -> new double[][]{p(-.7,-.4,-.3,-.5,.3,.25,.08,.04,0),p(-1.1,-.6,-.55,-.9,.4,.4,-.08,-.08,-.12),p(-.9,-.3,-.35,-1.1,.6,.55,-.05,-.08,.12),p(-.3,0,-.15,-.4,0,.15,.05,.02,0)};
            case STRETCH_LARGE -> new double[][]{p(-.8,-.2,-.45,-.6,.2,.4,.08,.06,0),p(-2.5,-.1,-.3,-2.3,.1,.3,-.18,-.12,-.06),p(-2.35,.1,-.25,-2.5,-.1,.3,-.1,-.1,.08),p(-.7,0,-.55,-.8,0,.55,.12,.04,0)};
            case CATCH -> new double[][]{p(-.5,-.1,-.2,0,0,0,.12,.06,0),p(-1.35,.2,.25,-.45,0,.2,.2,.1,-.05),p(-.85,-.3,-.1,-1.1,-.15,.12,.12,0,.03),p(-.45,0,0,-.25,0,0,0,0,0)};
            case BRUSH -> new double[][]{p(-1.1,-.7,.5,-.3,0,.15,.15,.05,0),p(-1.0,-.35,.25,-.3,0,.15,.15,.05,0),p(-.5,0,-.1,-.35,0,.1,.3,.3,0),p(-.7,.1,-.15,-.5,-.1,.15,.2,.15,0)};
            case SHADE -> new double[][]{p(-1.6,-.1,.25,0,0,0,-.08,0,0),p(-2.4,-.3,.35,0,0,0,-.12,0,.02),p(-2.3,-.25,.3,0,0,0,-.08,0,0),p(-1.2,0,.1,0,0,0,0,0,0)};
            case BALANCE -> new double[][]{p(-.35,0,-.9,-.15,0,.7,.1,.12,.08),p(-.2,0,-1.2,-.4,0,1.0,0,.04,-.07),p(-.15,0,-.6,-.1,0,.45,0,0,.025),p(0,0,-.12,0,0,.08,0,0,0)};
            case WALL -> new double[][]{p(-.4,0,-.5,0,0,0,.06,0,.025),p(-.8,0,-1.05,0,0,0,0,0,.04),p(-.75,0,-1.05,0,0,0,0,0,.04),p(-.35,0,-.45,0,0,0,0,0,.015)};
            case SHAKE -> new double[][]{p(-.7,0,-.15,-.45,0,.1,.12,0,0),p(-.3,.1,.2,-.8,-.1,-.15,.08,0,0),p(-.85,-.1,-.2,-.25,.15,.2,0,0,0),p(-.3,.1,.12,-.55,-.1,-.12,0,0,0)};
            case POCKETS -> new double[][]{p(.05,-.1,.1,0,0,0,.25,.05,0),p(-.2,-.15,.2,0,0,0,.3,.08,-.025),p(0,0,0,-.15,.15,-.2,.25,.05,.025),p(-.1,-.1,.1,-.1,.1,-.1,.08,0,0)};
            case PROTECT -> new double[][]{p(-.8,-.2,.3,-.2,0,.1,.05,.06,0),p(-1.35,-.6,.55,-.75,.4,-.3,.1,.1,0),p(-1.25,-.55,.5,-.7,.35,-.3,.08,.08,0),p(-.5,-.15,.15,-.2,0,0,0,0,0)};
            case INSPECT -> new double[][]{p(-.7,0,-.05,-.1,0,0,.12,0,0),p(-1.35,.15,-.25,-.2,0,0,.2,0,0),p(-1.3,-.25,.2,-.15,0,0,.16,0,0),p(-.8,.1,-.05,0,0,0,.06,0,0)};
            case FIRE_PAT -> new double[][]{p(-.5,-.5,.3,-.2,.2,-.1,.2,.1,0),p(-.9,-.7,.4,-.4,.3,-.2,.3,.12,-.04),p(-.3,-.2,.1,-.9,.65,-.4,.25,.1,.04),p(-.6,-.4,.2,-.4,.3,-.2,.1,.03,0)};
            case FREEZE_RUB,END_CHILL -> new double[][]{p(-.7,-.6,.4,-.7,.6,-.4,.15,.04,0),p(-1,-.8,.55,-.9,.7,-.5,.2,.06,.02),p(-.8,-.65,.45,-1,.8,-.55,.2,.06,-.02),p(-.5,-.4,.25,-.5,.4,-.25,.1,.02,0)};
            case NETHER_SWEAT -> new double[][]{p(-1.5,-.2,.2,-.1,0,.05,.08,.02,0),p(-2.35,-.3,.35,-.1,0,.05,-.05,0,.02),p(-2.25,.25,.25,-.1,0,.05,-.04,0,-.02),p(-.7,.1,.1,-.1,0,.03,.08,.02,0)};
            case WARM -> new double[][]{p(-.9,-.1,-.1,-.8,.1,.1,.12,.08,0),p(-1.25,-.1,-.15,-1.15,.1,.15,.15,.1,0),p(-1.1,-.45,.3,-1.15,.45,-.3,.12,.06,0),p(-.9,-.3,.2,-1.0,.3,-.2,.1,.02,0)};
            case FLINCH -> new double[][]{p(-2.0,-.25,.5,-.5,0,.3,-.12,-.08,-.08),p(-1.7,-.2,.4,-.45,0,.2,-.08,-.04,-.04),p(-.8,0,.15,-.2,0,.1,.03,0,.02),p(-.2,0,0,0,0,0,0,0,0)};
        };
    }
    private double[] beats() {
        return switch(this) {
            case FISH_CAST -> new double[]{0,4,7,11,16,24};
            case FISH_REEL -> new double[]{0,3,8,13,21,30};
            case FISH_BITE -> new double[]{0,2,5,9,14,20};
            case INSPECT_SPECIAL_SWORD -> new double[]{0,16,40,65,86,110};
            case INSPECT_SPECIAL_GEAR, INSPECT_TREASURE -> new double[]{0,duration*.18,duration*.35,duration*.62,duration*.78,duration};
            case FLINCH -> new double[]{0,1,3,6,10,16};
            case CATCH -> new double[]{0,3,6,10,16,24};
            case BRUSH -> new double[]{0,8,15,25,34,48};
            case SHAKE -> new double[]{0,5,10,16,23,34};
            default -> new double[]{0,duration*.14,duration*.29,duration*.49,duration*.70,duration};
        };
    }
    public static double wallRoll(double distance,double scale) {
        double length=10*.9375*scale/16,shoulder=5*.9375*scale/16;
        return -Math.asin(Math.max(0,Math.min(1,(distance-shoulder)/length)));
    }
    private MotionCurve[] curves;
    private boolean supportHand;
    public boolean usesSupportHand(){channels();return supportHand;}
    private synchronized MotionCurve[] channels() {
        if(curves==null) {
            var keys=poses();
            supportHand=java.util.Arrays.stream(keys).anyMatch(key->Math.abs(key[3])+Math.abs(key[4])+Math.abs(key[5])>.001);
            curves=new MotionCurve[10]; var times=beats();
            for(int c=0;c<9;c++) curves[c]=new MotionCurve(times,0,keys[0][c],keys[1][c],keys[2][c],keys[3][c],0);
            curves[9]=new MotionCurve(times,0,.75,1,1,.65,0);
        }
        return curves;
    }
    public Pose sample(double time,int variant) {
        double t=Math.max(0,Math.min(duration,time));var c=channels();
        // Slightly different recovery accents; server transmits the same variation to all viewers.
        double accent=variant%2==0?1:.94;
        double breath=(this==SPRINT_RECOVERY||this==INJURED_REST||this==CAMPFIRE_REST)?Math.sin(t*.28)*.025*c[9].at(t):0;
        double strength=this==SPRINT_RECOVERY?.65+.12*variant:1;
        return new Pose(c[0].at(t)*strength+breath,c[1].at(t)*accent,c[2].at(t),c[3].at(t)*strength-breath,c[4].at(t)*accent,
                c[5].at(t),c[6].at(t),c[7].at(t)*strength+breath*.4,c[8].at(t),Math.max(0,Math.min(1,c[9].at(t))));
    }
}
