package dev.shtark.rpganimation.animation;

/** Cosmetic status only. No chat, commands, item data or cursor positions. */
public final class ActivityState {
    public static final int NONE=0, TYPING=1, BROWSING=2, IDLE=3;
    public static final int MIN_INTERVAL=5, HEARTBEAT=20, EXPIRY=100;
    public static boolean valid(int kind,int intensity) {
        return kind>=NONE&&kind<=IDLE&&intensity>=0&&intensity<=3
                &&(kind==TYPING||kind==BROWSING||intensity==0);
    }
    public static double blend(double age) {
        double t=Math.clamp(age/10,0,1);return t*t*(3-2*t);
    }
    // Short pauses between keys keep the same loop, rather than restarting its blend.
    public static int typingIntensity(long quietTicks) {return quietTicks>=0&&quietTicks<30?2:0;}
    public static double handHandoff(double weight) {double t=Math.clamp(weight/.2,0,1);return t*t*(3-2*t);}
    public static double typingLift(double weight) {double t=Math.clamp((weight-.2)/.8,0,1);return t*t*(3-2*t);}
    public record Pose(double right,double left,double head,double roll) {}
    public static Pose pose(int kind,int intensity,double ticks) {
        double beat=Math.sin(ticks*(.25+intensity*.09));
        double typingBeat=Math.sin(ticks*.43);
        return switch(kind) {
            case TYPING -> new Pose(-.72+typingBeat*.045*intensity,-.66-typingBeat*.035*intensity,.16,.015*Math.sin(ticks*.07));
            case BROWSING -> new Pose(-1.05+beat*.07*intensity,-.85,.30,.02*Math.sin(ticks*.035));
            case IDLE -> new Pose(.08,.06,.62+.24*Math.sin(ticks*.035),.045*Math.sin(ticks*.017));
            default -> new Pose(0,0,0,0);
        };
    }
    private ActivityState() {}
}
