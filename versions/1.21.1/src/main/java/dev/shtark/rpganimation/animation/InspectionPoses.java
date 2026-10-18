package dev.shtark.rpganimation.animation;

/** Original raise, study, turn/test and recovery poses. */
final class InspectionPoses {
    private InspectionPoses() {}
    static double[][] keys(ContextClip clip) {
        return switch(clip) {
            case INSPECT_SWORD -> new double[][]{
                {-1.0,-.2,.15,0,0,0,.08,0,0}, {-1.65,-.3,.25,0,0,0,-.12,0,0},
                {-1.35,.25,-.2,0,0,0,.18,.02,.02}, {-.65,0,.08,0,0,0,.04,0,0}};
            case INSPECT_AXE -> new double[][]{
                {-.9,-.25,.15,0,0,0,.1,0,0}, {-1.4,-.5,.35,0,0,0,.15,.02,-.02},
                {-1.6,.3,-.3,0,0,0,-.06,0,.02}, {-.6,-.1,.08,0,0,0,.03,0,0}};
            case INSPECT_PICKAXE -> new double[][]{
                {-1,-.15,.1,0,0,0,.1,0,0}, {-1.5,-.4,.5,0,0,0,.05,0,-.02},
                {-1.45,.4,-.4,0,0,0,.12,.02,.02}, {-.6,0,.1,0,0,0,.04,0,0}};
            case INSPECT_SHOVEL -> new double[][]{
                {-.9,-.1,.12,0,0,0,.12,0,0}, {-1.35,-.2,.2,0,0,0,.2,.03,0},
                {-1.55,.2,-.25,0,0,0,-.08,0,0}, {-.6,0,.05,0,0,0,.04,0,0}};
            case INSPECT_HOE -> new double[][]{
                {-.85,-.25,.2,0,0,0,.08,0,0}, {-1.45,-.45,.35,0,0,0,.16,.02,0},
                {-1.25,.35,-.45,0,0,0,.08,0,.03}, {-.6,0,.1,0,0,0,.03,0,0}};
            case INSPECT_SPECIAL_SWORD -> new double[][]{
                {-1.5,-.3,.25,0,0,0,.23,.02,0}, {-1.85,-.2,.1,0,0,0,-.25,-.02,-.015},
                {-1.25,.45,-.5,-.25,.15,-.1,.12,.04,.045}, {-.9,-.3,.3,-.1,0,0,.06,0,-.02}};
            case INSPECT_SPECIAL_TOOL -> new double[][]{
                {-1.2,-.25,.2,-.7,.3,-.2,.12,.02,0}, {-1.6,-.3,.35,-1.2,.45,-.3,.02,0,0},
                {-1.4,.25,-.3,-.6,.15,-.1,.15,.02,.02}, {-.7,-.1,.08,-.3,.1,-.08,.04,0,0}};
            case INSPECT_GEAR -> presentation(-1.25, .12);
            case INSPECT_SPECIAL_GEAR -> presentation(-1.5, .18);
            case INSPECT_TREASURE -> presentation(-1.65, -.06);
            default -> throw new IllegalArgumentException("Not an inspection style: "+clip);
        };
    }
    private static double[][] presentation(double lift,double look) {
        return new double[][]{
            {-.9,-.35,.28,-.85,.35,-.28,.1,.02,0},
            {lift,-.45,.38,lift+.05,.45,-.38,look,.02,0},
            {lift,-.42,.35,lift+.05,.42,-.35,look,.02,.008},
            {-.7,-.2,.16,-.65,.2,-.16,.05,0,0}};
    }
}
