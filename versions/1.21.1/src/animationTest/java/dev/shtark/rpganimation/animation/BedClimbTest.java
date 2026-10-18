package dev.shtark.rpganimation.animation;

/** Tests contact using rendered rigid-limb endpoints, not timeline labels. */
public final class BedClimbTest {
    public static void run() {
        double[][] directions = {{90,0,1},{0,-1,0},{270,0,-1},{180,1,0}};
        for (double[] d : directions) for (double[] origin : new double[][]{{1,0},{-1,0},{0,1},{0,-2}}) {
            var planted = scene(d, origin, 20);
            var right = limb(planted, -5, 2, 10, planted.arm());
            var left = limb(planted, 5, 2, 10, planted.arm());
            for (double t = 20; t <= 54; t += .1) {
                var s = scene(d, origin, t);
                near(limb(s, -5, 2, 10, s.arm()), right, "Right hand stationary during support");
                near(limb(s, 5, 2, 10, s.arm()), left, "Left hand stationary during support");
                check(Math.abs(right.y() - .5625) < 1e-7 && Math.abs(left.y() - .5625) < 1e-7,
                        "Hands contact mattress");
                check(s.arm() - s.transform().tilt() < 0, "Hands reach forward, not back toward the hips");
                if (t <= 40) check(s.arm() < -1, "Visible arm lift during reach and weight transfer");
                inView(s, right);
                inView(s, left);
                if (t >= 34) check(Math.abs(limb(s, -1.9, 12, 12, s.leadLeg()).y() - .5625) < 1e-7,
                        "Leading foot remains on mattress while trailing leg enters");
            }
            var first = scene(d, origin, 34);
            double firstFoot = limb(first, -1.9, 12, 12, first.leadLeg()).y();
            double rearFoot = limb(first, 1.9, 12, 12, first.trailLeg()).y();
            check(firstFoot - rearFoot > .45, "One leg enters before the second");
            var previous = scene(d, origin, 0);
            for (double t = .1; t <= BedEntrySequence.DURATION; t += .1) {
                var s = scene(d, origin, t);
                check(limb(s, -1.9, 12, 12, s.leadLeg()).y() >= -1e-6, "Leading foot above floor at " + t);
                check(limb(s, 1.9, 12, 12, s.trailLeg()).y() >= -1e-6, "Trailing foot above floor at " + t);
                check(Double.isFinite(s.arm()) && Math.abs(s.leadLeg() - previous.leadLeg()) < .09
                        && Math.abs(s.trailLeg() - previous.trailLeg()) < .09, "Continuous limb rotations");
                previous = s;
            }
            var end = scene(d, origin, BedEntrySequence.DURATION);
            check(Math.abs(end.leadLeg()) < 1e-7 && Math.abs(end.trailLeg()) < 1e-7
                    && end.arm() == 0 && end.head() == 0, "Neutral final limbs");
        }
        check(BedEntrySequence.DURATION < 100, "Climb ends before normal sleep threshold");
        check(BedEntrySequence.sample(20).support() == 1 && BedEntrySequence.sample(20).leadLift() == 0,
                "Hands brace before the first leg climbs");
        check(BedEntrySequence.sample(54).trailLift() == 1 && BedEntrySequence.sample(54).seat() == 0,
                "Both legs enter before the seated pivot");
        System.out.println("Passed: forward arm reach, fixed palms, camera hand visibility bounds, sequential legs, floor clearance and neutral finish in four orientations");
    }

    private static BedGeometry.Scene scene(double[] d, double[] origin, double time) {
        return BedGeometry.scene(origin[0], 0, origin[1], 0, .6875, 0, 75, d[0], (int)d[1], (int)d[2], 1.62, 1, time);
    }

    // Apply limb Rx, model scale/axis flip/translation, renderer Rx/Ry and world translation.
    private static BedGeometry.Point limb(BedGeometry.Scene scene, double px, double py, double length, double angle) {
        double unit = BedGeometry.MODEL_SCALE / 16;
        double x = -px * unit;
        double y = BedGeometry.MODEL_SCALE * 1.501 - (py + length * Math.cos(angle)) * unit;
        double z = length * Math.sin(angle) * unit;
        var t = scene.transform();
        double ry = y * Math.cos(t.tilt()) - z * Math.sin(t.tilt());
        double rz = y * Math.sin(t.tilt()) + z * Math.cos(t.tilt());
        double yaw = Math.toRadians(t.yaw());
        return new BedGeometry.Point(t.x() + x * Math.cos(yaw) + rz * Math.sin(yaw),
                .6875 + t.y() + ry, t.z() - x * Math.sin(yaw) + rz * Math.cos(yaw));
    }
    private static void near(BedGeometry.Point a, BedGeometry.Point b, String message) {
        check(Math.abs(a.x()-b.x()) < 1e-7 && Math.abs(a.y()-b.y()) < 1e-7 && Math.abs(a.z()-b.z()) < 1e-7, message);
    }
    private static void inView(BedGeometry.Scene s, BedGeometry.Point hand) {
        double yaw = Math.toRadians(s.transform().yaw()), pitch = Math.toRadians(72);
        double x = hand.x() - s.eye().x(), y = hand.y() - s.eye().y(), z = hand.z() - s.eye().z();
        double along = x * Math.sin(yaw) + z * Math.cos(yaw);
        double forward = -along * Math.cos(pitch) - y * Math.sin(pitch);
        double up = -along * Math.sin(pitch) + y * Math.cos(pitch);
        double side = x * Math.cos(yaw) - z * Math.sin(yaw);
        check(forward > .1 && Math.abs(up / forward) < Math.tan(Math.toRadians(35))
                && Math.abs(side / forward) < Math.tan(Math.toRadians(35)) * 16 / 9,
                "Bracing palms within 70-degree 16:9 camera view before mesh occlusion");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
