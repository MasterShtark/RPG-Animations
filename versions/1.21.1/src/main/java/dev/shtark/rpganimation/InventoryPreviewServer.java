package dev.shtark.rpganimation;

import java.util.*;
import dev.shtark.rpganimation.animation.InventoryPreview;
import dev.shtark.rpganimation.animation.ByteBudget;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=RpgAnimation.ID)
public final class InventoryPreviewServer {
    private static final Map<UUID,dev.shtark.rpganimation.animation.PreviewCadence> NEXT=new HashMap<>();
    private static final Map<UUID,Frame> FRAMES=new HashMap<>();
    private static final Map<UUID,PreviewPolicyPayload> POLICIES=new HashMap<>();
    private static final ByteBudget OUTPUT=new ByteBudget();
    private static final class Frame {
        InventoryPreviewPayload image;long received;int version,sourceRevision;
        final Map<UUID,Integer> recipients=new HashMap<>();
    }
    private static List<ServerPlayer> audience(ServerPlayer p) {
        var result=new ArrayList<ServerPlayer>();
        for(var viewer:p.level().getServer().getPlayerList().getPlayers()) {
            var sub=ActivityServer.subscription(viewer.getUUID());
            if(viewer==p||sub==null||!sub.previews()||viewer.level()!=p.level())continue;
            int range=Math.min(24,Math.min(sub.distance(),ServerConfig.ACTIVITY_RANGE.get()));
            if(viewer.distanceToSqr(p)<=range*range&&p.hasLineOfSight(viewer)) {
                result.add(viewer);if(result.size()>=ServerConfig.PREVIEW_RECIPIENTS.get())break;
            }
        }
        return result;
    }
    public static void subscriptionChanged(ServerPlayer viewer) {
        // A reset client may have released textures even when its preferences are unchanged.
        for(var entry:FRAMES.entrySet())if(entry.getValue().recipients.remove(viewer.getUUID())!=null)
            PacketDistributor.sendToPlayer(viewer,new InventoryPreviewPayload(entry.getKey(),0,0,new byte[0]));
    }
    public static void accept(ServerPlayer p,InventoryPreviewRequest request) {
        if(!InventoryPreview.valid(request.width(),request.height(),request.pixels().length))return;
        // An idempotent clear never competes with image upload cooldowns.
        if(request.width()==0){clear(p);return;}
        long now=p.level().getServer().getTickCount();
        if(!NEXT.computeIfAbsent(p.getUUID(),id->new dev.shtark.rpganimation.animation.PreviewCadence())
                .acquire(now,ServerConfig.PREVIEW_INTERVAL.get()))return;
        var policy=POLICIES.get(p.getUUID());
        if(!ServerConfig.ACTIVITY.get()||!ServerConfig.PREVIEWS.get()||policy==null||!policy.allowed()||!p.isAlive()||p.isSpectator())return;
        var frame=FRAMES.get(p.getUUID());
        if(request.width()==-1){
            if(frame!=null&&frame.sourceRevision==request.revision())frame.received=now;
            else PacketDistributor.sendToPlayer(p,policy); // Ask for a full refresh after a dropped/rate-limited image.
            return;
        }
        if(request.width()>ServerConfig.PREVIEW_WIDTH.get())return;
        if(frame==null){frame=new Frame();FRAMES.put(p.getUUID(),frame);}
        if(frame.image==null||frame.image.width()!=request.width()||frame.image.height()!=request.height()
                ||!Arrays.equals(frame.image.pixels(),request.pixels())) {
            frame.image=new InventoryPreviewPayload(p.getUUID(),request.width(),request.height(),request.pixels());frame.version++;
        }
        frame.sourceRevision=request.revision();frame.received=now;
    }
    private static void clear(ServerPlayer p) {
        var frame=FRAMES.remove(p.getUUID());if(frame==null)return;
        for(var id:frame.recipients.keySet()) {
            var viewer=p.level().getServer().getPlayerList().getPlayer(id);
            if(viewer!=null)PacketDistributor.sendToPlayer(viewer,new InventoryPreviewPayload(p.getUUID(),0,0,new byte[0]));
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server=event.getServer();long now=server.getTickCount();if(now%5!=0)return;
        var players=new ArrayList<>(server.getPlayerList().getPlayers());
        // Rotate who uses the shared budget first so a crowded scene does not always favour the first login.
        if(!players.isEmpty())Collections.rotate(players,(int)(now/20%players.size()));
        for(var p:players) {
            var senderSubscription=ActivityServer.subscription(p.getUUID());
            boolean enabled=senderSubscription!=null&&senderSubscription.sharingPreview()&&ServerConfig.ACTIVITY.get()&&ServerConfig.PREVIEWS.get()&&p.isAlive()&&!p.isSpectator();
            var viewers=enabled?audience(p):List.<ServerPlayer>of();
            var policy=new PreviewPolicyPayload(enabled&&!viewers.isEmpty(),ServerConfig.PREVIEW_WIDTH.get(),ServerConfig.PREVIEW_INTERVAL.get());
            if(!policy.equals(POLICIES.put(p.getUUID(),policy)))PacketDistributor.sendToPlayer(p,policy);
            var frame=FRAMES.get(p.getUUID());if(frame==null)continue;
            if(!policy.allowed()||now-frame.received>ServerConfig.PREVIEW_INTERVAL.get()*3L+40
                    ||frame.image.width()>ServerConfig.PREVIEW_WIDTH.get()){clear(p);continue;}
            var wanted=new HashSet<UUID>();
            for(var viewer:viewers) {
                var id=viewer.getUUID();wanted.add(id);
                if(frame.recipients.getOrDefault(id,-1)!=frame.version) {
                    if(!OUTPUT.take(now/20,frame.image.pixels().length+32,ServerConfig.PREVIEW_BYTES.get()))continue;
                    PacketDistributor.sendToPlayer(viewer,frame.image);frame.recipients.put(id,frame.version);
                } else if(now%20==0) PacketDistributor.sendToPlayer(viewer,new InventoryPreviewPayload(p.getUUID(),-1,0,new byte[0]));
            }
            for(var id:new ArrayList<>(frame.recipients.keySet()))if(!wanted.contains(id)) {
                var viewer=server.getPlayerList().getPlayer(id);
                if(viewer!=null)PacketDistributor.sendToPlayer(viewer,new InventoryPreviewPayload(p.getUUID(),0,0,new byte[0]));
                frame.recipients.remove(id);
            }
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);NEXT.remove(p.getUUID());POLICIES.remove(p.getUUID());}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){clear(p);NEXT.remove(p.getUUID());POLICIES.remove(p.getUUID());}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){NEXT.clear();FRAMES.clear();POLICIES.clear();OUTPUT.reset();}
}
