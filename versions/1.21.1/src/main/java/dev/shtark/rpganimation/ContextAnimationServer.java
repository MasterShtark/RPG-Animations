package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.ContextClip;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-observed context only; never moves a player, changes an item or cancels gameplay. */
@EventBusSubscriber(modid=RpgAnimation.ID)
public final class ContextAnimationServer {
    private record Run(ContextClip clip,long start,int variant,String item,Vec3 origin) {
        ContextPayload payload(ServerPlayer p) {return new ContextPayload(p.getUUID(),clip.ordinal(),
                (int)(p.level().getGameTime()-start),variant,true,origin.x,origin.y,origin.z);}
    }
    private static final class State {
        final EnumMap<ContextClip,Long> next=new EnumMap<>(ContextClip.class);
        Vec3 previous; int dark,wet,waterTicks,idle; long brightUntil,requestAfter; int pickups;
        final dev.shtark.rpganimation.animation.IdleSchedule stretches;
        Vec3 idlePosition; float idleYaw,idlePitch;
        Vec3 wallPosition; float wallYaw;long wallTick=Long.MIN_VALUE;Boolean rightWall,leftWall;
        int fishingHand; boolean biting; String inspectedItem;
        State(UUID id){stretches=new dev.shtark.rpganimation.animation.IdleSchedule(id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),23));}
    }
    private static final Map<UUID,Run> ACTIVE=new HashMap<>();
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static State state(ServerPlayer p){return STATES.computeIfAbsent(p.getUUID(),State::new);}
    private static boolean eligible(ServerPlayer p) { return eligible(p,null); }
    private static boolean eligible(ServerPlayer p,ContextClip clip) {
        return p.isAlive() && !p.isSpectator() && !p.isSleeping() && (!p.isPassenger()||(clip==ContextClip.SEAT_ENTER&&RpgDetailsServer.isSeat(p.getVehicle()))) && !p.isFallFlying()
                && !p.isUsingItem() && (p.hurtTime==0 || (clip==ContextClip.FIRE_PAT && p.isOnFire())) && !BedAnimationServer.isRunning(p)
                && !IntroServer.isRunning(p) && !DropAnimationServer.isRunning(p);
    }
    public static boolean active(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
    private static boolean rod(ServerPlayer p,int hand) {
        return p.getItemInHand(hand%2==0?net.minecraft.world.InteractionHand.MAIN_HAND:net.minecraft.world.InteractionHand.OFF_HAND)
                .canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST);
    }
    public static void fishingUsed(ServerPlayer p,net.minecraft.world.InteractionHand hand) {
        var s=state(p);s.fishingHand=hand==net.minecraft.world.InteractionHand.MAIN_HAND?0:1;
        // Vanilla has already spawned or removed the hook; this never changes its outcome.
        var old=ACTIVE.get(p.getUUID());
        if(old!=null&&old.clip.fishing())stop(p);
        begin(p,p.fishing!=null?ContextClip.FISH_CAST:ContextClip.FISH_REEL,s.fishingHand);
        s.biting=false;s.stretches.tick(false);
    }
    public static boolean begin(ServerPlayer p,ContextClip clip,int variant) {return begin(p,clip,variant,p.position());}
    static boolean begin(ServerPlayer p,ContextClip clip,int variant,Vec3 origin) {
        if(!ServerConfig.CONTEXT.get()||!ServerConfig.GESTURES.get(clip).get()||!RpgDetailsServer.canStart(p,clip))return false;
        if(clip.inspection()&&(!ServerConfig.GESTURES.get(ContextClip.INSPECT).get()||p.getMainHandItem().isEmpty()
                ||!p.getOffhandItem().isEmpty()||p.swinging||BasicActionServer.busy(p)))return false;
        long now=p.level().getGameTime();var state=state(p);
        var old=ACTIVE.get(p.getUUID());
        if(!dev.shtark.rpganimation.animation.ContextPolicy.mayStart(eligible(p,clip),now,
                state.next.getOrDefault(clip,0L),old==null?null:old.clip,clip))return false;
        state.next.put(clip,now+clip.cooldown);
        var run=new Run(clip,now,Math.floorMod(variant,4),BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString(),origin);ACTIVE.put(p.getUUID(),run);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,run.payload(p));return true;
    }
    private static void stop(ServerPlayer p) {
        if(ACTIVE.remove(p.getUUID())!=null)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new ContextPayload(p.getUUID(),0,0,0,false));
    }
    public static boolean pickup(ServerPlayer p,net.minecraft.world.item.ItemStack picked) {
        if((picked.getRarity()==Rarity.RARE || picked.getRarity()==Rarity.EPIC)
                && net.minecraft.world.item.ItemStack.isSameItemSameComponents(picked,p.getMainHandItem()))
            if(inspect(p))return true;
        var s=state(p);
        return ++s.pickups%7==0 && begin(p,ContextClip.CATCH,s.pickups%4);
    }
    private static boolean inspect(ServerPlayer p) {
        var item=p.getMainHandItem();
        if(item.isEmpty()||!p.onGround()||p.isSprinting()||p.isUsingItem()||p.swinging
                ||!p.getOffhandItem().isEmpty()||p.containerMenu!=p.inventoryMenu||ActivityServer.interacting(p)
                ||state(p).idle<20||active(p))return false;
        String id=BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        if(id.equals(state(p).inspectedItem))return false;
        var seen=p.getData(RpgAnimation.DISCOVERIES);
        if(seen.contains(id) || seen.size()>=8192){state(p).inspectedItem=id;return false;}
        if(!begin(p,ItemInspection.style(item),p.tickCount%4))return false;
        var updated=new ArrayList<>(seen);updated.add(id);p.setData(RpgAnimation.DISCOVERIES,List.copyOf(updated));state(p).inspectedItem=id;return true;
    }
    public static void pockets(ServerPlayer p) {
        if(!ServerConfig.CONTEXT.get())return;
        var state=state(p);long now=p.level().getGameTime();
        if(now<state.requestAfter)return;state.requestAfter=now+20;
        if(!eligible(p)||!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty())return;
        Vec3 from=p.getEyePosition();
        var hit=p.level().clip(new ClipContext(from,from.add(p.getLookAngle().scale(5)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        if(hit.getType()==HitResult.Type.MISS)begin(p,ContextClip.POCKETS,p.tickCount%4);
    }
    private static boolean solid(ServerPlayer p,BlockPos pos) {
        return p.level().hasChunkAt(pos)&&!p.level().getBlockState(pos).getCollisionShape(p.level(),pos).isEmpty();
    }
    private static boolean edge(ServerPlayer p) {
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})
            if(!solid(p,BlockPos.containing(p.getX()+x*.65,p.getY()-.3,p.getZ()+z*.65)))return true;
        return false;
    }
    private static boolean wall(ServerPlayer p,double side) {
        var cache=state(p);long now=p.level().getServer().getTickCount();
        if(cache.wallPosition==null||!cache.wallPosition.equals(p.position())||cache.wallYaw!=p.getYRot()||now-cache.wallTick>=5) {
            cache.wallPosition=p.position();cache.wallYaw=p.getYRot();cache.wallTick=now;cache.rightWall=cache.leftWall=null;
        }
        Boolean value=side>0?cache.rightWall:cache.leftWall;if(value!=null)return value;
        double yaw=Math.toRadians(p.getYRot());Vec3 from=p.position().add(0,1.15*p.getScale(),0);
        Vec3 to=from.add(-Math.cos(yaw)*side*.7,0,-Math.sin(yaw)*side*.7);
        boolean hit=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.BLOCK;
        if(side>0)cache.rightWall=hit;else cache.leftWall=hit;return hit;
    }
    static boolean fire(ServerPlayer p) {
        BlockPos origin=p.blockPosition();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=1;y++) {
            var pos=origin.offset(x,y,z);if(!p.level().hasChunkAt(pos))continue;
            var block=p.level().getBlockState(pos);
            if((block.is(Blocks.CAMPFIRE)||block.is(Blocks.SOUL_CAMPFIRE))&&block.getValue(CampfireBlock.LIT)
                    && p.position().distanceToSqr(Vec3.atCenterOf(pos))<6.25
                    && p.getLookAngle().dot(Vec3.atCenterOf(pos).subtract(p.getEyePosition()).normalize())>.2)return true;
        }
        return false;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e) {
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(!ServerConfig.CONTEXT.get()){stop(p);STATES.remove(p.getUUID());return;}
        long now=p.level().getGameTime();var run=ACTIVE.get(p.getUUID());var activity=state(p);
        boolean moved=activity.idlePosition!=null&&activity.idlePosition.distanceToSqr(p.position())>.0001;
        boolean still=activity.idlePosition!=null&&activity.idlePosition.distanceToSqr(p.position())<.000001
                &&Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot()-activity.idleYaw))<.1
                &&Math.abs(p.getXRot()-activity.idlePitch)<.1;
        activity.idlePosition=p.position();activity.idleYaw=p.getYRot();activity.idlePitch=p.getXRot();
        boolean interacting=ActivityServer.interacting(p);
        boolean quiet=!interacting&&still&&eligible(p)&&p.onGround()&&!p.isInWater()&&!p.isInLava()&&!p.isCrouching()
                &&!p.swinging&&p.fishing==null&&p.containerMenu==p.inventoryMenu
                &&p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty();
        var stretch=activity.stretches.tick(quiet&&(run==null||run.clip.stretch()));
        if(run!=null&&run.clip.stretch()&&!quiet){stop(p);run=null;}
        if(interacting&&run!=null&&(run.clip==ContextClip.WARM||run.clip.inspection()||run.clip.environmental())){stop(p);run=null;}
        if(stretch!=null)begin(p,stretch,p.getRandom().nextInt(4));
        if(run!=null && (!ServerConfig.GESTURES.get(run.clip).get()||!RpgDetailsServer.keepPlaying(p,run.clip)||!eligible(p,run.clip)||now-run.start>=run.clip.duration
                || (run.clip.fishing()&&(!rod(p,run.variant)
                    ||((run.clip==ContextClip.FISH_WAIT||run.clip==ContextClip.FISH_BITE)&&p.fishing==null)))
                || (run.clip.inspection()&&(p.swinging||BasicActionServer.busy(p)||!p.getOffhandItem().isEmpty()))
                || (run.clip.inspection()&&!run.item.equals(BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString()))
                || ((run.clip==ContextClip.WARM||run.clip.environmental())&&p.getDeltaMovement().horizontalDistanceSqr()>.001)
                || (run.clip==ContextClip.FIRE_PAT&&!p.isOnFire())
                || (run.clip.environmental()&&moved)
                || (run.clip==ContextClip.FREEZE_RUB&&p.getTicksFrozen()==0)))stop(p);
        if(p.fishing!=null&&eligible(p)&&rod(p,activity.fishingHand)) {
            boolean bite=((dev.shtark.rpganimation.mixin.FishingHookAccessor)p.fishing).rpgAnimation$isBiting();
            if(bite&&!activity.biting)begin(p,ContextClip.FISH_BITE,activity.fishingHand);
            activity.biting=bite;
            if(p.tickCount%20==0&&p.fishing.isInWater()&&!active(p))begin(p,ContextClip.FISH_WAIT,activity.fishingHand);
        } else activity.biting=false;
        int stagger=p.tickCount+Math.floorMod(p.getUUID().hashCode(),20);
        if(stagger%5!=0)return;
        run=ACTIVE.get(p.getUUID());
        if(run!=null&&((run.clip==ContextClip.WALL&&!wall(p,run.variant%2==0?1:-1))
                ||(run.clip==ContextClip.PROTECT&&(p.getMainHandItem().isEmpty()||!wall(p,1)||!wall(p,-1)))
                ||(run.clip==ContextClip.WARM&&stagger%20==0&&!fire(p))))stop(p);
        var s=state(p);boolean moving=s.previous!=null&&s.previous.distanceToSqr(p.position())>.015;
        s.previous=p.position();s.idle=moving||interacting?0:Math.min(400,s.idle+5);
        boolean exposed=p.level().canSeeSky(p.blockPosition().above())&&p.level().isDay();
        if(!exposed)s.dark=Math.min(200,s.dark+5);
        else {if(s.dark>=40)s.brightUntil=now+40;s.dark=0;}
        if(p.isInWater()){s.waterTicks=Math.min(100,s.waterTicks+5);if(s.waterTicks>=20)s.wet=100;return;}
        s.waterTicks=0;
        if(!eligible(p,p.isOnFire()?ContextClip.FIRE_PAT:null))return;
        if(p.fishing!=null||interacting)return;
        if(!moving&&p.onGround()&&s.idle>=20&&p.containerMenu==p.inventoryMenu) {
            if(p.isOnFire()&&begin(p,ContextClip.FIRE_PAT,p.tickCount%4))return;
            if(p.getTicksFrozen()>0&&begin(p,ContextClip.FREEZE_RUB,p.tickCount%4))return;
            if(s.idle>=100&&stagger%20==0) {
                if(p.level().dimension().equals(net.minecraft.world.level.Level.NETHER)&&begin(p,ContextClip.NETHER_SWEAT,p.tickCount%4))return;
                if(p.level().dimension().equals(net.minecraft.world.level.Level.END)&&begin(p,ContextClip.END_CHILL,p.tickCount%4))return;
            }
        }
        if(s.wet>0){s.wet-=5;if(p.onGround()&&begin(p,ContextClip.SHAKE,p.tickCount%4)){s.wet=0;return;}}
        if(exposed && now<s.brightUntil && p.getXRot()<-10 && begin(p,ContextClip.SHADE,p.tickCount%4))return;
        if(inspect(p))return;
        if(moving&&p.onGround()) {
            boolean right=wall(p,1),left=wall(p,-1);
            if(right&&left&&!p.getMainHandItem().isEmpty())begin(p,ContextClip.PROTECT,p.tickCount%4);
            else if((right||left)&&p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty())
                begin(p,ContextClip.WALL,right?0:1);
        }
        if(s.idle>=60&&stagger%20==0&&p.onGround()&&p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty()
                && p.level().getBiome(p.blockPosition()).value().coldEnoughToSnow(p.blockPosition())
                && fire(p))begin(p,ContextClip.WARM,p.tickCount%4);
    }
    @SubscribeEvent public static void fall(LivingFallEvent e) {
        if(ServerConfig.CONTEXT.get()&&e.getEntity() instanceof ServerPlayer p && e.getDistance()>1.25 && edge(p))begin(p,ContextClip.BALANCE,p.tickCount%4);
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void impact(ProjectileImpactEvent e) {
        if(!ServerConfig.CONTEXT.get())return;
        if(!(e.getProjectile().level() instanceof net.minecraft.server.level.ServerLevel level))return;
        var hit=e.getRayTraceResult();if(hit.getType()==HitResult.Type.MISS)return;
        for(ServerPlayer p:level.getEntitiesOfClass(ServerPlayer.class,e.getProjectile().getBoundingBox().inflate(3)))
            if(p!=e.getProjectile().getOwner()&&p.position().distanceToSqr(hit.getLocation())<6.25)
                begin(p,ContextClip.FLINCH,p.tickCount%4);
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        if(e.getEntity() instanceof ServerPlayer viewer&&e.getTarget() instanceof ServerPlayer p) {
            var run=ACTIVE.get(p.getUUID());if(run!=null)PacketDistributor.sendToPlayer(viewer,run.payload(p));
        }
    }
    @SubscribeEvent public static void untracking(PlayerEvent.StopTracking e) {
        if(e.getEntity() instanceof ServerPlayer viewer&&e.getTarget() instanceof ServerPlayer p)
            PacketDistributor.sendToPlayer(viewer,new ContextPayload(p.getUUID(),0,0,0,false));
    }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p) {
        var run=ACTIVE.get(p.getUUID());
        if(run!=null&&run.clip==ContextClip.FIRE_PAT&&e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE))return;
        stop(p);
    }}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){stop(p);STATES.remove(p.getUUID());}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){stop(p);STATES.remove(p.getUUID());}}
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e) {
        var root=net.minecraft.commands.Commands.literal("rpganimation");
        var gestures=net.minecraft.commands.Commands.literal("gesture");
        for(var clip:ContextClip.values())gestures.then(net.minecraft.commands.Commands.literal(clip.name().toLowerCase(java.util.Locale.ROOT))
                .executes(c->{var p=c.getSource().getPlayerOrException();if(begin(p,clip==ContextClip.INSPECT?ItemInspection.style(p.getMainHandItem()):clip,p.tickCount%4))return 1;
                    c.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("rpg_animation.gesture.unavailable"));return 0;}));
        root.then(gestures);e.getDispatcher().register(root);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){ACTIVE.clear();STATES.clear();}
    static void resetAfterTeleport(ServerPlayer p) { stop(p);STATES.remove(p.getUUID()); }
}


