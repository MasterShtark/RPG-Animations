package dev.shtark.rpganimation.animation;

import java.util.*;

/** Sender-owned, bounded-lived cosmetic state; independent of client classes. */
public final class ActivityLedger {
    public record Status(int kind,int intensity,long received) {}
    private final Map<UUID,Status> states=new HashMap<>();
    private final Map<UUID,Long> next=new HashMap<>();
    public boolean update(UUID sender,int kind,int intensity,long now) {
        if(!ActivityState.valid(kind,intensity)||now<next.getOrDefault(sender,Long.MIN_VALUE))return false;
        next.put(sender,now+ActivityState.MIN_INTERVAL);
        if(kind==0)states.remove(sender);else states.put(sender,new Status(kind,intensity,now));
        return true;
    }
    public boolean expired(UUID id,long now) {
        var s=states.get(id);return s!=null&&now-s.received>=ActivityState.EXPIRY;
    }
    public boolean clear(UUID id){next.remove(id);return states.remove(id)!=null;}
    public Status get(UUID id){return states.get(id);}
    public Map<UUID,Status> snapshot(){return Map.copyOf(states);}
    public void reset(){states.clear();next.clear();}
}
