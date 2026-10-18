package dev.shtark.rpganimation.client.pose;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HumanoidModel;

/** Synthetic parent for older models whose six body parts have no shared root. */
public final class ModelRoot {
    private static final Map<Object,ModelPart> ROOTS = new WeakHashMap<>();
    private static final Map<Object,Overlay> OVERLAYS = new WeakHashMap<>();
    private ModelRoot() {}
    public static ModelPart of(Object model) {
        return ROOTS.computeIfAbsent(model,key->new ModelPart(List.of(),Map.of()));
    }

    /** Keep additive poses local to one draw on models reused between players. */
    public static void capture(HumanoidModel<?> model) {
        var overlay = OVERLAYS.computeIfAbsent(model, key -> new Overlay(model));
        overlay.capture();
    }

    public static void restore(Object model) {
        var overlay = OVERLAYS.get(model);
        if (overlay != null) overlay.restore();
    }

    private static final class Overlay {
        private final ModelPart[] parts;
        private final float[][] values;
        private boolean captured;
        Overlay(HumanoidModel<?> model) {
            parts = new ModelPart[]{model.head, model.hat, model.body, model.rightArm,
                    model.leftArm, model.rightLeg, model.leftLeg};
            values = new float[parts.length][9];
        }
        void capture() {
            for (int i = 0; i < parts.length; i++) {
                var p = parts[i]; var v = values[i];
                v[0]=p.x; v[1]=p.y; v[2]=p.z;
                v[3]=p.xRot; v[4]=p.yRot; v[5]=p.zRot;
                v[6]=p.xScale; v[7]=p.yScale; v[8]=p.zScale;
            }
            captured = true;
        }
        void restore() {
            if (!captured) return;
            for (int i = 0; i < parts.length; i++) {
                var p = parts[i]; var v = values[i];
                p.setPos(v[0],v[1],v[2]); p.setRotation(v[3],v[4],v[5]);
                p.xScale=v[6]; p.yScale=v[7]; p.zScale=v[8];
            }
            captured = false;
        }
    }
}
