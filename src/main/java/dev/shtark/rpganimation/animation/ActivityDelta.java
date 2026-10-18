package dev.shtark.rpganimation.animation;

import java.util.*;

/** Reliable connections carry changes and explicit removals, not repeated status heartbeats. */
public final class ActivityDelta {
    public record Value(int kind,int intensity) {}
    public static Map<UUID,Value> between(Map<UUID,Value> previous,Map<UUID,Value> desired) {
        return between(previous,desired,false);
    }
    public static Map<UUID,Value> between(Map<UUID,Value> previous,Map<UUID,Value> desired,boolean refresh) {
        var delta=new LinkedHashMap<UUID,Value>();
        desired.forEach((id,value)->{if(refresh||!value.equals(previous.get(id)))delta.put(id,value);});
        previous.keySet().forEach(id->{if(!desired.containsKey(id))delta.put(id,new Value(0,0));});
        return delta;
    }
    private ActivityDelta() {}
}
