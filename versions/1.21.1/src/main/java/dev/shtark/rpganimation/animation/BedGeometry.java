package dev.shtark.rpganimation.animation;

/** Original choreography using the installed vanilla avatar's rigid limb lengths. */
public final class BedGeometry {
    private BedGeometry() {}
    public static final double MODEL_SCALE = .9375;
    public static final double HIP = 1.501 - 12.0 / 16;
    public record Point(double x, double y, double z) {
        public Point add(double dx, double dy, double dz) { return new Point(x + dx, y + dy, z + dz); }
        public Point lerp(Point to, double amount) {
            return new Point(x + (to.x - x) * amount, y + (to.y - y) * amount, z + (to.z - z) * amount);
        }
    }
    public record Transform(double x, double y, double z, double yaw, double tilt) {}
    public record Scene(Transform transform, Point hip, Point eye, double leadLeg, double trailLeg,
                        double arm, double head, double leftArm) {}

    public static double angleDelta(double from, double to) {
        double d = (to - from) % 360;
        return d < -180 ? d + 360 : d >= 180 ? d - 360 : d;
    }
    private static double acos(double value) { return Math.acos(Math.max(-1, Math.min(1, value))); }

    public static Transform sample(double ox, double oy, double oz, double bx, double by, double bz,
                                   double initialYaw, double sleepAngle, int dx, int dz,
                                   double eyeHeight, double scale, double time) {
        return scene(ox, oy, oz, bx, by, bz, initialYaw, sleepAngle, dx, dz, eyeHeight, scale, time).transform;
    }

    public static Scene scene(double ox, double oy, double oz, double bx, double by, double bz,
                              double initialYaw, double sleepAngle, int dx, int dz,
                              double eyeHeight, double scale, double time) {
        var f = BedEntrySequence.sample(time);
        double unit = MODEL_SCALE * scale / 16;
        double hipLength = HIP * MODEL_SCALE * scale, armLength = 10 * unit, legLength = 12 * unit;
        double mattress = by - .125; // LivingEntity.setPosToBed adds .125 above sleepHeight.
        double offset = eyeHeight - .1;
        Point endHip = new Point(bx - dx * (offset - hipLength), by, bz - dz * (offset - hipLength));

        // Face into the mattress from the actual approach side, including either end.
        double nx = ox - endHip.x, nz = oz - endHip.z, distance = Math.hypot(nx, nz);
        if (distance < .001) { nx = -dx; nz = -dz; }
        else { nx /= distance; nz /= distance; }
        double entryYaw = Math.toDegrees(Math.atan2(nx, nz));
        double yaw = 180 - initialYaw + angleDelta(180 - initialYaw, entryYaw) * f.turn();
        yaw += angleDelta(yaw, sleepAngle + 270) * f.seat();

        // Solve the hip from two fixed palms and rigid arms, rather than moving the
        // palms with a linearly sliding body. No elbow bones or limb stretching.
        var planted = BedEntrySequence.sample(Math.min(54, time));
        // Negative world arm angle reaches toward the bed. The previous positive
        // branch pointed the hands back toward the hips and read as resting arms.
        double theta = planted.supportAngle();
        double reach = -.12 + armLength * Math.sin(planted.tilt()) + armLength * Math.sin(theta);
        Point braceHip = new Point(endHip.x - nx * reach,
                mattress - armLength * Math.cos(planted.tilt()) + armLength * Math.cos(theta), endHip.z - nz * reach);
        Point hip = new Point(ox, oy + hipLength, oz).lerp(braceHip, f.approach()).lerp(endHip, f.seat());

        double r = Math.toRadians(yaw);
        double tilt = f.tilt();
        Transform transform = new Transform(hip.x - bx - hipLength * Math.sin(tilt) * Math.sin(r),
                hip.y - by - hipLength * Math.cos(tilt), hip.z - bz - hipLength * Math.sin(tilt) * Math.cos(r), yaw, tilt);

        double groundAngle = acos((hip.y - oy) / legLength);
        double footPlane = mattress + .125 * f.recline();
        double raisedAngle = acos((hip.y - footPlane) / legLength);
        double lead = -(groundAngle + (raisedAngle - groundAngle) * f.leadLift()) - Math.max(0, f.stride());
        double trail = groundAngle + (-raisedAngle - groundAngle) * f.trailLift() + Math.max(0, -f.stride());
        double arm = (planted.tilt() + theta) * f.support();
        double head = -.32 * f.support();
        double headLength = eyeHeight - hipLength;
        Point eye = hip.add(headLength * Math.sin(tilt) * Math.sin(r), headLength * Math.cos(tilt),
                headLength * Math.sin(tilt) * Math.cos(r));
        return new Scene(transform, hip, eye, tilt + lead, tilt + trail, arm, head, arm);
    }
}
