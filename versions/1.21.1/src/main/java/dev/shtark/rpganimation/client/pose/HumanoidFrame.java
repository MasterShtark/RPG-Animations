package dev.shtark.rpganimation.client.pose;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.HumanoidArm;

public class HumanoidFrame {
    private final Map<PoseKey<?>, Object> data = new HashMap<>();
    public HumanoidArm mainArm;

    @SuppressWarnings("unchecked")
    public <T> T getRenderData(PoseKey<T> key) { return (T) data.get(key); }
    public <T> void setRenderData(PoseKey<T> key, T value) {
        if (value == null) data.remove(key); else data.put(key, value);
    }
}
