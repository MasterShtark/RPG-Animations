package dev.shtark.rpganimation.animation;

/** Rigid rotation in the model Y/Z plane about the hip origin. */
public final class HipBend {
    public record Point(double y,double z) {}
    public static Point rotate(double y,double z,double angle) {
        double c=Math.cos(angle),s=Math.sin(angle);
        return new Point(y*c-z*s,y*s+z*c);
    }
    private HipBend() {}
}
