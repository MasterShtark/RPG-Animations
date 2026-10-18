package dev.shtark.rpganimation.animation;

public final class BedGeometryTest {
    public static void run() {
        double[][] directions = {{90,0,1},{0,-1,0},{270,0,-1},{180,1,0}};
        for (double[] d : directions) for (double yaw : new double[]{-179,-90,0,75,179,360}) {
            var start = BedGeometry.sample(2, 0, 3, 0, .6875, 0, yaw, d[0], (int)d[1], (int)d[2], 1.62, 1, 0);
            near(start.x(), 2, "Origin X"); near(start.y(), -.6875, "Origin Y"); near(start.z(), 3, "Origin Z");
            near(start.yaw(), 180-yaw, "Original facing"); near(start.tilt(), 0, "Standing start");
            var end = BedGeometry.sample(2, 0, 3, 0, .6875, 0, yaw, d[0], (int)d[1], (int)d[2], 1.62, 1, BedEntrySequence.DURATION);
            near(end.x(), -d[1]*1.52, "Vanilla sleeping offset X");
            near(end.y(), 0, "Vanilla sleeping height"); near(end.z(), -d[2]*1.52, "Vanilla sleeping offset Z");
            near(BedGeometry.angleDelta(end.yaw(), d[0]+270), 0, "Vanilla orientation");
            near(end.tilt(), Math.PI/2, "Vanilla recline");
            var prior = start;
            for (double t=.1; t<=BedEntrySequence.DURATION; t+=.1) {
                var v=BedGeometry.sample(2,0,3,0,.6875,0,yaw,d[0],(int)d[1],(int)d[2],1.62,1,t);
                check(Math.abs(v.x()-prior.x()) < .05 && Math.abs(v.y()-prior.y()) < .05
                        && Math.abs(v.z()-prior.z()) < .05, "Continuous bed approach");
                prior=v;
            }
        }
        var c = new MotionCurve(new double[]{0,10,20,30}, 0,1,2,2);
        double e=.001;
        for (int t : new int[]{10,20}) {
            double before=(c.at(t)-c.at(t-e))/e, after=(c.at(t+e)-c.at(t))/e;
            check(Math.abs(before-after)<.001,"Continuous curve velocity");
        }
        check((c.at(10+e)-c.at(10-e))/(2*e)>.05,"No forced pause at intermediate rising key");
        for(double t=0;t<=30;t+=.1) check(c.at(t)>=0 && c.at(t)<=2,"No curve overshoot");
                for (double key : new double[]{20,48,72,86,98,108}) {
            double h=.001;
            var a=GetUpSequence.sample(key-h); var b=GetUpSequence.sample(key); var next=GetUpSequence.sample(key+h);
            check(Math.abs((b.pitch()-a.pitch())/h - (next.pitch()-b.pitch())/h)<.001, "Wake torso velocity continuity");
            check(Math.abs((b.hipHeight()-a.hipHeight())/h - (next.hipHeight()-b.hipHeight())/h)<.001, "Wake hip velocity continuity");
        }
        System.out.println("Passed: captured-origin/vanilla-end bed transforms in four directions, continuous travel and curve velocities");
    }
    private static void near(double a,double b,String message){check(Math.abs(a-b)<1e-7,message+": "+a+" != "+b);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}

