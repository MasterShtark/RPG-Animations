package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.BasicAction;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Success-only notifications; no new client requests or gameplay modifications. */
@EventBusSubscriber(modid=RpgAnimation.ID)
public final class BasicActionServer {
    private record Run(BasicAction action,long start,boolean offhand,double x,double y,double z) {
        BasicActionPayload payload(ServerPlayer p){return new BasicActionPayload(p.getUUID(),action.ordinal(),(int)(p.level().getGameTime()-start),offhand,true,x,y,z);}
    }
    private static final Map<UUID,Run> ACTIVE=new HashMap<>();
    private static final class MotionState {
        boolean grounded,water,swimming,sleeping,gliding; String sophisticatedBackpack,travelersBackpack; double top; int sleepTicks,exitWaterTicks,flightLandingTicks; Entity vehicle;
        net.minecraft.world.phys.Vec3 position; long pageAfter;
    }
    private static final Map<UUID,MotionState> MOTION=new HashMap<>();
    public static void start(ServerPlayer p,BasicAction action,InteractionHand hand) {start(p,action,hand,p.position());}
    private static void start(ServerPlayer p,BasicAction action,InteractionHand hand,net.minecraft.world.phys.Vec3 origin) {
        if(!ServerConfig.BASIC_ACTIONS.get()||!p.isAlive()||p.isSpectator()||(p.isSleeping()&&action!=BasicAction.SLEEP_SETTLE)
                ||IntroServer.isRunning(p)||BedAnimationServer.isRunning(p))return;
        var old=ACTIVE.get(p.getUUID());long now=p.level().getGameTime();
        if(old!=null&&now-old.start<4)return;
        var run=new Run(action,now,hand==InteractionHand.OFF_HAND,origin.x,origin.y,origin.z);ACTIVE.put(p.getUUID(),run);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,run.payload(p));
    }
    public static void used(ServerPlayer p,InteractionHand hand,BlockState before,BlockState after,boolean openedMenu,boolean hoe) {
        BasicAction action=null;
        if(hoe&&!before.is(Blocks.FARMLAND)&&after.is(Blocks.FARMLAND))action=BasicAction.TILL;
        else if(before!=after&&before.getBlock()==after.getBlock()) {
            if(before.getBlock() instanceof ButtonBlock)action=BasicAction.BUTTON;
            else if(before.getBlock() instanceof LeverBlock)action=BasicAction.LEVER;
            else if(before.getBlock() instanceof DoorBlock)action=BasicAction.DOOR;
            else if(before.getBlock() instanceof TrapDoorBlock)action=BasicAction.TRAPDOOR;
            else if(before.getBlock() instanceof FenceGateBlock)action=BasicAction.GATE;
        }
        if(openedMenu) {
            if(before.getBlock() instanceof ChestBlock||before.is(Blocks.ENDER_CHEST))action=BasicAction.CHEST;
            else if(before.is(Blocks.BARREL))action=BasicAction.BARREL;
            else if(before.getBlock() instanceof ShulkerBoxBlock)action=BasicAction.SHULKER;
            else action=BasicAction.CONTAINER;
        }
        if(action!=null)start(p,action,hand);
    }
    public static boolean busy(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
    public static void mounted(ServerPlayer p,Entity vehicle) {
        if(!(vehicle instanceof LivingEntity))return;
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString();
        BasicAction action=dev.shtark.rpganimation.animation.MountCompatibility.mounting(id,ServerConfig.UNKNOWN_MOUNT_ANIMATIONS.get());
        if(action!=null)start(p,action,InteractionHand.MAIN_HAND);
    }
    public static void page(ServerPlayer p) {
        var s=MOTION.computeIfAbsent(p.getUUID(),id->new MotionState());long now=p.level().getGameTime();
        if(now<s.pageAfter)return;s.pageAfter=now+10;
        for(var hand:InteractionHand.values()) {
            var item=p.getItemInHand(hand);
            if(item.is(net.minecraft.world.item.Items.WRITTEN_BOOK)||item.is(net.minecraft.world.item.Items.WRITABLE_BOOK)) {start(p,BasicAction.PAGE,hand);return;}
        }
    }
    private static void transitions(ServerPlayer p) {
        var s=MOTION.get(p.getUUID());
        if(s==null){s=new MotionState();s.grounded=p.onGround();s.top=p.getY();s.water=p.isInWater();s.swimming=p.isSwimming();s.gliding=p.isFallFlying();s.sophisticatedBackpack=BackpackEquipment.sophisticated(p);s.travelersBackpack=BackpackEquipment.traveler(p);s.vehicle=p.getVehicle();s.position=p.position();MOTION.put(p.getUUID(),s);return;}
        if(ServerConfig.BASIC_ACTIONS.get()&&p.isAlive()&&!p.isSpectator()) {
            if(p.isInWater()||p.onClimbable()||p.isPassenger()||p.getAbilities().flying)s.top=p.getY();
            if(!p.onGround())s.top=Math.max(s.top,p.getY());
            if(p.isFallFlying())s.flightLandingTicks=20;
            else if(s.flightLandingTicks>0)s.flightLandingTicks--;
            if(p.isInWater()||p.isPassenger()||p.isSleeping()||p.getAbilities().flying)s.flightLandingTicks=0;
            boolean flightLanding=s.flightLandingTicks>0&&!p.isFallFlying()&&p.onGround();
            if(flightLanding){start(p,BasicAction.ELYTRA_LAND,InteractionHand.MAIN_HAND);s.flightLandingTicks=0;}
            if(!flightLanding&&p.onGround()&&!s.grounded&&!p.isInWater()&&!p.isPassenger()&&!p.getAbilities().flying&&s.top-p.getY()>.8)
                start(p,s.top-p.getY()>3?BasicAction.LAND_HARD:BasicAction.LAND_SOFT,InteractionHand.MAIN_HAND);
            if(!s.gliding&&p.isFallFlying())start(p,BasicAction.ELYTRA_TAKEOFF,InteractionHand.MAIN_HAND);
            if(Math.floorMod(p.tickCount+p.getUUID().hashCode(),5)==0) {
                String sophisticated=BackpackEquipment.sophisticated(p),traveler=BackpackEquipment.traveler(p);
                boolean equipped=dev.shtark.rpganimation.animation.EquipTransition.equipped(s.sophisticatedBackpack,sophisticated)
                        ||dev.shtark.rpganimation.animation.EquipTransition.equipped(s.travelersBackpack,traveler);
                s.sophisticatedBackpack=sophisticated;s.travelersBackpack=traveler;
                if(equipped)start(p,BasicAction.BACKPACK_EQUIP,InteractionHand.MAIN_HAND);
            }
            if(p.onGround())s.top=p.getY();
            if(!s.swimming&&p.isSwimming())start(p,BasicAction.SWIM_PUSH,InteractionHand.MAIN_HAND);
            if(s.water&&!p.isInWater())s.exitWaterTicks=10;
            if(s.exitWaterTicks>0){s.exitWaterTicks--;if(p.onGround()&&!p.isInWater()){start(p,BasicAction.SWIM_EXIT,InteractionHand.MAIN_HAND);s.exitWaterTicks=0;}}
            if(s.vehicle instanceof LivingEntity&&!p.isPassenger()&&s.vehicle.isAlive()&&s.position.distanceToSqr(p.position())<16) {
                String id=BuiltInRegistries.ENTITY_TYPE.getKey(s.vehicle.getType()).toString();
                BasicAction action=dev.shtark.rpganimation.animation.MountCompatibility.dismounting(id,ServerConfig.UNKNOWN_MOUNT_ANIMATIONS.get());
                if(action!=null)start(p,action,InteractionHand.MAIN_HAND,s.position);
            }
            s.sleepTicks=p.isSleeping()?s.sleepTicks+1:0;
            if(s.sleepTicks==70)start(p,BasicAction.SLEEP_SETTLE,InteractionHand.MAIN_HAND);
        }
        s.grounded=p.onGround();s.water=p.isInWater();s.swimming=p.isSwimming();s.gliding=p.isFallFlying();s.vehicle=p.getVehicle();s.position=p.position();
    }
    @SubscribeEvent public static void projectile(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if(e.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile arrow
                &&(arrow instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow||arrow instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity)
                &&arrow.getOwner() instanceof ServerPlayer p) {
            var weapon=arrow instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow a?a.getWeaponItem():null;
            boolean crossbow=weapon==null||weapon.getUseAnimation()==net.minecraft.world.item.ItemUseAnimation.CROSSBOW;
            var use=crossbow?net.minecraft.world.item.ItemUseAnimation.CROSSBOW:net.minecraft.world.item.ItemUseAnimation.BOW;
            if(weapon!=null&&weapon.getUseAnimation()!=use)return;
            boolean main=p.getMainHandItem().getUseAnimation()==use,off=p.getOffhandItem().getUseAnimation()==use;
            if(!main&&!off)return;
            var hand=main&&off?p.getUsedItemHand():off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            start(p,crossbow?BasicAction.CROSSBOW_RELEASE:BasicAction.BOW_RELEASE,hand);
        }
    }
    private static void stop(ServerPlayer p) {
        if(ACTIVE.remove(p.getUUID())!=null)PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new BasicActionPayload(p.getUUID(),0,0,false,false,0,0,0));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            transitions(p);
            var r=ACTIVE.get(p.getUUID());if(r==null)return;
            if(!ServerConfig.BASIC_ACTIONS.get()||!p.isAlive()||(p.isSleeping()&&r.action!=BasicAction.SLEEP_SETTLE)||(r.action==BasicAction.SLEEP_SETTLE&&!p.isSleeping())
                    ||(p.hurtTime>0&&r.action!=BasicAction.LAND_HARD&&r.action!=BasicAction.LAND_SOFT&&r.action!=BasicAction.ELYTRA_LAND&&r.action!=BasicAction.SHIELD_HIT)
                    ||(p.isUsingItem()&&r.action!=BasicAction.SHIELD_HIT&&r.action!=BasicAction.BOW_RELEASE&&r.action!=BasicAction.CROSSBOW_RELEASE)
                    ||(r.action==BasicAction.SHIELD_HIT&&!p.isBlocking())||(r.action.dismount()&&p.isPassenger())
                    ||(r.action.mount()&&!p.isPassenger())||p.level().getGameTime()-r.start>=r.action.duration)stop(p);
        }
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        if(e.getEntity() instanceof ServerPlayer viewer&&e.getTarget() instanceof ServerPlayer p) {
            var r=ACTIVE.get(p.getUUID());if(r!=null)PacketDistributor.sendToPlayer(viewer,r.payload(p));
        }
    }
    @SubscribeEvent public static void untracking(PlayerEvent.StopTracking e) {
        if(e.getEntity() instanceof ServerPlayer viewer&&e.getTarget() instanceof ServerPlayer p)
            PacketDistributor.sendToPlayer(viewer,new BasicActionPayload(p.getUUID(),0,0,false,false,0,0,0));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){stop(p);MOTION.remove(p.getUUID());}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){stop(p);MOTION.remove(p.getUUID());}}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){if(e.getEntity() instanceof ServerPlayer p){stop(p);MOTION.remove(p.getUUID());}}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){ACTIVE.clear();MOTION.clear();}
    static void resetAfterTeleport(ServerPlayer p) { stop(p);MOTION.remove(p.getUUID()); }
}
