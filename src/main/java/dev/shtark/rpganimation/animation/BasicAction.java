package dev.shtark.rpganimation.animation;

/** Original rigid Minecraft limb poses. Angles in radians, normalized action time. */
public enum BasicAction {
    SWING(12), CHOP(16), DIG(15), PICK(14), TILL(16),
    DOOR(12), TRAPDOOR(14), GATE(12), CHEST(18), BARREL(16), SHULKER(18), CONTAINER(14),
    HORSE(26), DONKEY(28), CAMEL(34), LLAMA(30), PIG(22), STRIDER(26), OTHER_MOUNT(28),
    LAND_SOFT(16), LAND_HARD(26), CLIMB(24), SWIM_PUSH(20), SWIM_LOOP(28), SWIM_EXIT(26),
    DISMOUNT_HORSE(26), DISMOUNT_CAMEL(34), DISMOUNT_SMALL(22),
    SHIELD(16), SHIELD_HIT(12), BOW_DRAW(20), BOW_RELEASE(14), CROSSBOW_LOAD(25), CROSSBOW_AIM(20), CROSSBOW_RELEASE(16),
    BUTTON(12), LEVER(16), PAGE(20), PLACE(12), SLEEP_SETTLE(28), ELYTRA_TAKEOFF(20), ELYTRA_FLIGHT(40), ELYTRA_LAND(24), BACKPACK_EQUIP(32);
    public final int duration;
    BasicAction(int duration) { this.duration=duration; }
    public boolean mount() { return ordinal()>=HORSE.ordinal()&&ordinal()<=OTHER_MOUNT.ordinal(); }
    public boolean dismount(){return this==DISMOUNT_HORSE||this==DISMOUNT_CAMEL||this==DISMOUNT_SMALL;}
    public boolean continuous(){return this==ELYTRA_FLIGHT||this==CLIMB||this==SWIM_LOOP||this==SHIELD||this==BOW_DRAW||this==CROSSBOW_LOAD||this==CROSSBOW_AIM;}
    public record Pose(double arm,double yaw,double roll,double support,double lean,double leg,double otherLeg,double weight) {}
    private final MotionCurve weight=new MotionCurve(new double[]{0,.12,.34,.58,.82,1},0,.65,1,1,.55,0);
    public Pose sample(double phase) {
        double t=Math.max(0,Math.min(1,phase)), w=weight.at(t);
        double wind=pulse(t,0,.2,.42), contact=pulse(t,.16,.42,.75), recover=pulse(t,.42,.65,1);
        double a=0,y=0,z=0,s=0,l=0,leg=0,other=0;
        switch(this) {
            case ELYTRA_TAKEOFF -> {a=-.7*wind+.25*contact;s=-.7*wind+.25*contact;z=-.6*contact;l=-.08*wind;leg=.2*contact;other=.2*contact;}
            case ELYTRA_LAND -> {a=-.8*wind-.25*contact;s=-.7*wind-.2*contact;z=-.3*wind;l=.2*contact;leg=-.45*contact;other=-.35*contact;}
            case BACKPACK_EQUIP -> {a=-2.1*wind-.8*contact;s=-.6*wind-1.9*contact;y=.35*contact;z=.3*wind;l=.12*contact;}
            case SWING -> {a=-1.3*wind-.55*contact;y=-.5*wind+.6*contact;z=-.22*wind;l=.06*contact;}
            case CHOP -> {a=-2.2*wind-.7*contact;y=-.25*wind+.35*contact;z=-.35*wind;s=-.3*wind;l=-.08*wind+.16*contact;}
            case DIG -> {a=-.65*wind-1.2*contact-.8*recover;y=.2*wind-.28*recover;s=-.3*contact;l=.2*contact;}
            case PICK -> {a=-2.5*wind-.9*contact;z=-.15*wind;s=-.2*contact;l=-.05*wind+.12*contact;}
            case TILL -> {a=-.7*wind-1.25*contact-.4*recover;y=.2*wind-.2*recover;l=.2*contact;s=-.25*contact;}
            case DOOR -> {a=-1.25*contact;y=.18*contact;s=-.15*wind;l=.06*contact;}
            case TRAPDOOR -> {a=-.6*wind-1.55*contact;y=-.2*contact;l=.14*wind;}
            case GATE -> {a=-1.2*contact;y=.55*recover;z=-.1*contact;l=.05*contact;}
            case CHEST -> {a=-.65*wind-1.65*contact;s=-.55*wind-1.45*contact;l=.16*wind-.04*recover;}
            case BARREL -> {a=-1.15*contact-.65*recover;y=.25*recover;s=-.3*contact;l=.08*contact;}
            case SHULKER -> {a=-1.45*contact;s=-1.25*contact;z=-.25*recover;l=.12*wind;}
            case CONTAINER -> {a=-1.2*contact;y=-.15*recover;l=.08*contact;}
            case LAND_SOFT,LAND_HARD -> {
                double strength=this==LAND_HARD?1:.45;
                double sink=pulse(t,0,.18,.72),settle=pulse(t,.35,.63,1);
                a=-.55*sink*strength;s=-.4*sink*strength;z=-.35*sink*strength;
                l=(.48*sink-.07*settle)*strength;leg=-.75*sink*strength;other=-.68*sink*strength;
            }
            case SWIM_PUSH -> {a=-1.8*wind-.2*contact;s=-1.65*wind-.15*contact;z=-.45*contact;l=.12*wind;leg=-.25*contact;other=.22*contact;}
            case SWIM_EXIT -> {a=-1.4*wind-.7*contact;s=-1.2*wind-.8*contact;l=.35*contact;leg=-.8*wind;other=-.6*contact;}
            case DISMOUNT_HORSE,DISMOUNT_CAMEL,DISMOUNT_SMALL -> {double h=this==DISMOUNT_CAMEL?1.2:this==DISMOUNT_SMALL?.65:1;
                a=-1.1*wind-.6*contact;s=-1.35*wind-.7*contact;l=.25*wind*h;
                leg=-1.2*pulse(t,0,.24,.7)*h;other=-.55*pulse(t,.18,.48,.88)*h;z=-.2*contact;y=.25*wind;}
            case SHIELD_HIT -> {double hit=pulse(t,0,.12,.65);a=-.35*hit;y=-.18*hit;l=-.12*hit;s=-.1*hit;}
            case BOW_RELEASE -> {a=-.65*wind;y=.4*wind;s=-.35*contact;l=-.04*wind;}
            case CROSSBOW_RELEASE -> {a=-.45*wind;s=-.35*wind;l=-.06*wind;}
            case BUTTON -> {a=-1.2*contact;y=.08*contact;s=-.08*wind;}
            case LEVER -> {a=-1.3*wind-.7*contact;y=.15*wind-.18*recover;l=.05*contact;}
            case PAGE -> {a=-.9*wind-1.1*contact;y=-.4*wind+.4*contact;z=.2*contact;s=-.95*(wind+contact)*.6;}
            case PLACE -> {a=-1.05*contact;y=.14*wind;z=-.1*recover;l=.035*contact;}
            case SLEEP_SETTLE -> {a=-.12*wind-.06*recover;s=-.08*contact;z=-.08*wind;l=.025*wind-.02*recover;}
            case ELYTRA_FLIGHT,CLIMB,SWIM_LOOP,SHIELD,BOW_DRAW,CROSSBOW_LOAD,CROSSBOW_AIM -> {return continuousPose(t*duration,weight.at(t));}
            default -> {
                double height=switch(this){case CAMEL ->1.25;case PIG ->.65;case STRIDER ->.9;default ->1;};
                a=-1.4*wind-1.15*contact;s=-1.1*wind-1.4*contact;
                l=(.3*wind+.4*contact-.1*recover)*height;
                leg=-1.05*pulse(t,.05,.3,.68)*height;
                other=-.9*pulse(t,.27,.57,.9)*height;
                y=(this==DONKEY||this==LLAMA?-.25:-.1)*contact;
                z=(this==STRIDER?-.2:-.1)*wind;
            }
        }
        return new Pose(a,y,z,s,l,leg,other,w);
    }
    public Pose continuousPose(double phase,double blend) {
        double w=Math.max(0,Math.min(1,blend)), a=0,s=0,y=0,z=0,l=0,leg=0,other=0;
        switch(this) {
            case ELYTRA_FLIGHT -> {double breath=Math.sin(phase*.12);a=.12+.025*breath;s=.12-.025*breath;z=-.32;l=.015*breath;leg=.08;other=.08;}
            case CLIMB -> {double cycle=phase*Math.PI/12;a=-1.9-.6*Math.sin(cycle);s=-1.9+.6*Math.sin(cycle);leg=-.5-.45*Math.sin(cycle+1);other=-.5+.45*Math.sin(cycle+1);l=.06;}
            case SWIM_LOOP -> {double cycle=phase*Math.PI/14;a=-1.25-1.1*Math.sin(cycle);s=-1.25+1.1*Math.sin(cycle);z=-.18;leg=.22*Math.sin(cycle*2);other=-leg;l=.03*Math.sin(cycle);}
            case SHIELD -> {a=-1.15;y=-.35;z=.15;s=-.15;l=.025;}
            case BOW_DRAW -> {double draw=MotionCurve.ease(phase/20);a=-1.35-.15*draw;s=-1.45;y=-.55*draw;z=.08*draw;l=-.025*draw;}
            case CROSSBOW_LOAD -> {double draw=MotionCurve.ease(phase/25);a=-1.1;s=-.6-.7*draw;y=-.25;z=.12*Math.sin(draw*Math.PI);}
            case CROSSBOW_AIM -> {a=-1.4;s=-1.3;y=-.16;}
            default -> throw new IllegalStateException("Not a continuous action: "+this);
        }
        return new Pose(a*w,y*w,z*w,s*w,l*w,leg*w,other*w,w);
    }
    public static double pulse(double t,double start,double peak,double end) {
        if(t<=start||t>=end)return 0;
        return t<peak?MotionCurve.ease((t-start)/(peak-start)):1-MotionCurve.ease((t-peak)/(end-peak));
    }
}
