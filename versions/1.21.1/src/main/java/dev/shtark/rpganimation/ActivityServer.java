package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.ActivityState;
import dev.shtark.rpganimation.animation.ActivityDelta;
import dev.shtark.rpganimation.animation.ActivityDelta.Value;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=RpgAnimation.ID)
public final class ActivityServer {
    private static final dev.shtark.rpganimation.animation.ActivityLedger LEDGER=new dev.shtark.rpganimation.animation.ActivityLedger();
    private static final Map<UUID,SubscriptionRequest> SUBSCRIPTIONS=new HashMap<>();
    private static final Set<UUID> REFRESH=new HashSet<>();
    private static boolean dirty;
    private static final Map<UUID,Long> NEXT_SUBSCRIPTION=new HashMap<>();
    private static final Map<UUID,Map<UUID,Value>> SENT=new HashMap<>();
    public static boolean interacting(ServerPlayer player) {
        var state=LEDGER.get(player.getUUID());
        return state!=null&&!LEDGER.expired(player.getUUID(),player.level().getServer().getTickCount())
                &&(state.kind()==ActivityState.TYPING||state.kind()==ActivityState.BROWSING);
    }
    public static SubscriptionRequest subscription(UUID id){return SUBSCRIPTIONS.get(id);}
    public static void subscribe(ServerPlayer p,SubscriptionRequest request) {
        if(request.equals(SUBSCRIPTIONS.get(p.getUUID())))return;
        long now=p.level().getServer().getTickCount();
        if(now<NEXT_SUBSCRIPTION.getOrDefault(p.getUUID(),Long.MIN_VALUE))return;
        NEXT_SUBSCRIPTION.put(p.getUUID(),now+20);REFRESH.add(p.getUUID());dirty=true;
        InventoryPreviewServer.subscriptionChanged(p);
        SUBSCRIPTIONS.put(p.getUUID(),new SubscriptionRequest(request.generation(),request.activity(),request.tab(),request.previews(),request.sharingPreview(),Math.clamp(request.distance(),4,64)));
    }
    public static void accept(ServerPlayer p,ActivityRequest request) {
        if(!ActivityState.valid(request.kind(),request.intensity()))return;
        long now=p.level().getServer().getTickCount();
        int kind=ServerConfig.ACTIVITY.get()&&p.isAlive()&&!p.isSpectator()?request.kind():ActivityState.NONE;
        if(kind==ActivityState.IDLE&&p.fishing!=null)kind=ActivityState.NONE;
        var previous=LEDGER.get(p.getUUID());
        int intensity=kind==0?0:request.intensity();
        if(LEDGER.update(p.getUUID(),kind,intensity,now)
                &&(previous==null?kind!=0:previous.kind()!=kind||previous.intensity()!=intensity))dirty=true;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server=event.getServer();long now=server.getTickCount();
        if(now%5!=0)return;
        var states=LEDGER.snapshot();
        for(var entry:states.entrySet()) {
            var p=server.getPlayerList().getPlayer(entry.getKey());
            if(!ServerConfig.ACTIVITY.get()||p==null||!p.isAlive()||p.isSpectator()||LEDGER.expired(entry.getKey(),now)
                    ||(entry.getValue().kind()==ActivityState.IDLE&&p.fishing!=null)){LEDGER.clear(entry.getKey());dirty=true;}
        }
        if(!dirty&&now%20!=0)return;
        dirty=false;states=LEDGER.snapshot();
        for(var viewer:server.getPlayerList().getPlayers()) {
            var sub=SUBSCRIPTIONS.get(viewer.getUUID());
            var previous=SENT.computeIfAbsent(viewer.getUUID(),id->new HashMap<>());
            var desired=new HashMap<UUID,Value>();
            if(sub!=null&&(sub.activity()||sub.tab()))for(var entry:states.entrySet()) {
                var subject=server.getPlayerList().getPlayer(entry.getKey());if(subject==null)continue;
                int range=Math.min(sub.distance(),ServerConfig.ACTIVITY_RANGE.get());
                boolean nearby=sub.activity()&&subject.level()==viewer.level()&&subject.distanceToSqr(viewer)<=range*range;
                if(nearby||sub.tab())desired.put(entry.getKey(),new Value(entry.getValue().kind(),nearby?entry.getValue().intensity():0));
            }
            var changes=new ArrayList<ActivityPayload>();
            ActivityDelta.between(previous,desired,REFRESH.remove(viewer.getUUID())).forEach((id,value)->changes.add(new ActivityPayload(id,value.kind(),value.intensity(),now)));
            for(int i=0;i<changes.size();i+=128)PacketDistributor.sendToPlayer(viewer,new ActivityBatchPayload(changes.subList(i,Math.min(i+128,changes.size()))));
            SENT.put(viewer.getUUID(),desired);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){
        var id=e.getEntity().getUUID();LEDGER.clear(id);dirty=true;REFRESH.remove(id);SUBSCRIPTIONS.remove(id);NEXT_SUBSCRIPTION.remove(id);SENT.remove(id);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){LEDGER.clear(e.getEntity().getUUID());dirty=true;}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){LEDGER.reset();REFRESH.clear();dirty=false;SUBSCRIPTIONS.clear();NEXT_SUBSCRIPTION.clear();SENT.clear();}
}
