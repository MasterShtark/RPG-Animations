package dev.shtark.rpganimation;

import dev.shtark.rpganimation.animation.ContextClip;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Bounded observations of completed actions; never changes gameplay state. */
@EventBusSubscriber(modid=RpgAnimation.ID)
public final class RpgDetailsServer {
    private static final class State {
        int pendingUntil;
        final dev.shtark.rpganimation.animation.DetailActivity history=new dev.shtark.rpganimation.animation.DetailActivity();
        boolean seated; Vec3 previous,pendingOrigin; ContextClip pending;
    }
    private static final Map<UUID,State> STATES=new HashMap<>();
    private RpgDetailsServer() {}
    public static boolean isSeat(Entity entity) {
        return entity!=null&&ServerConfig.SEAT_TYPES.get().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
    private static boolean empty(ServerPlayer p){return p.getMainHandItem().isEmpty()&&p.getOffhandItem().isEmpty();}
    private static boolean still(ServerPlayer p){return p.getDeltaMovement().horizontalDistanceSqr()<.0004&&!p.isSprinting();}
    private static boolean workplace(ContextClip c){return c==ContextClip.CRAFT||c==ContextClip.ANVIL_WORK||c==ContextClip.ENCHANT_WORK||c==ContextClip.BREW_WORK;}
    private static boolean equipment(ContextClip c){return c==ContextClip.ADJUST_HELMET||c==ContextClip.ADJUST_CHEST||c==ContextClip.ADJUST_LEGS||c==ContextClip.ADJUST_BOOTS;}
    public static boolean canStart(ServerPlayer p,ContextClip c) {
        if(!c.detail())return true;
        if(p.isSwinging()||BasicActionServer.busy(p))return false;
        if(c==ContextClip.SEAT_ENTER)return isSeat(p.getVehicle());
        if(c==ContextClip.SEAT_EXIT)return !p.isPassenger();
        if(c==ContextClip.CAMPFIRE_REST)return p.onGround()&&still(p)&&empty(p)&&ContextAnimationServer.fire(p);
        return !p.isPassenger()&&(workplace(c)||equipment(c)||!ActivityServer.interacting(p));
    }
    public static boolean keepPlaying(ServerPlayer p,ContextClip c) {
        if(!c.detail())return true;
        if(!canStart(p,c))return false;
        if(c==ContextClip.SEAT_ENTER)return isSeat(p.getVehicle());
        if(c==ContextClip.SEAT_EXIT)return !p.isPassenger();
        if(c==ContextClip.CAREFUL_CROUCH)return p.isCrouching()&&p.onGround()&&empty(p);
        if((c==ContextClip.SURFACE_WIPE||c==ContextClip.RAIN_SHIELD)&&!empty(p))return false;
        if(c==ContextClip.RAIN_SHIELD&&!p.level().isRainingAt(p.blockPosition().above()))return false;
        if(c==ContextClip.INJURED_REST&&p.getHealth()>p.getMaxHealth()*.3f)return false;
        if(!workplace(c)&&(!p.onGround()||!still(p)))return false;
        return true;
    }
    private static boolean edge(ServerPlayer p) {
        Vec3 forward=p.getLookAngle().multiply(1,0,1).normalize().scale(.8);
        BlockPos pos=BlockPos.containing(p.position().add(forward).add(0,-.3,0));
        return p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).getCollisionShape(p.level(),pos).isEmpty();
    }
    private static boolean begin(ServerPlayer p,ContextClip c,int variant){return ContextAnimationServer.begin(p,c,variant);}
    private static void queue(ServerPlayer p,ContextClip c) {
        if(!ServerConfig.CONTEXT.get()||!ServerConfig.GESTURES.get(c).get())return;
        State s=STATES.get(p.getUUID());if(s==null)return;
        s.pending=c;s.pendingOrigin=p.position();s.pendingUntil=p.tickCount+40;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        if(!ServerConfig.CONTEXT.get()||!p.isAlive()||p.isSpectator()){STATES.remove(p.getUUID());return;}
        State s=STATES.get(p.getUUID());
        if(s==null){s=new State();s.seated=isSeat(p.getVehicle());s.previous=p.position();STATES.put(p.getUUID(),s);return;}
        Vec3 previous=s.previous;boolean moving=previous.distanceToSqr(p.position())>.0004;s.previous=p.position();
        boolean seated=isSeat(p.getVehicle());
        if(seated!=s.seated){queue(p,seated?ContextClip.SEAT_ENTER:ContextClip.SEAT_EXIT);s.pendingOrigin=previous;s.seated=seated;}
        s.history.tick(p.tickCount,p.isSprinting(),moving,p.onGround(),p.isUnderWater());
        int stagger=p.tickCount+Math.floorMod(p.getUUID().hashCode(),5);
        if(stagger%5!=0)return;
        if(s.pending!=null){
            if(p.tickCount>s.pendingUntil)s.pending=null;
            else if(ContextAnimationServer.begin(p,s.pending,p.tickCount%4,s.pendingOrigin)){s.pending=null;return;}
        }
        if(p.isPassenger()||!p.onGround()||p.isInWater()||p.isInLava()||p.isUsingItem()||p.isSwinging()
                ||p.fishing!=null||ActivityServer.interacting(p)||p.containerMenu!=p.inventoryMenu)return;
        if(p.isCrouching()&&empty(p)&&edge(p)){begin(p,ContextClip.CAREFUL_CROUCH,p.tickCount%4);return;}
        if(moving||!still(p))return;
        if(s.history.breath(p.tickCount)>=0&&begin(p,ContextClip.SPRINT_RECOVERY,s.history.breath(p.tickCount))){s.history.breathed();return;}
        if(s.history.wipe(p.tickCount)&&empty(p)&&begin(p,ContextClip.SURFACE_WIPE,0)){s.history.wiped();return;}
        if(p.getHealth()<=p.getMaxHealth()*.3f&&begin(p,ContextClip.INJURED_REST,p.tickCount%4))return;
        if(empty(p)&&p.level().isRainingAt(p.blockPosition().above())&&begin(p,ContextClip.RAIN_SHIELD,p.tickCount%4))return;
        if(p.getXRot()>20&&edge(p))begin(p,ContextClip.LEDGE_LOOK,p.tickCount%4);
    }
    @SubscribeEvent public static void equip(LivingEquipmentChangeEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||e.getTo().isEmpty()||e.getFrom().getItem()==e.getTo().getItem())return;
        ContextClip clip=switch(e.getSlot()) {
            case HEAD -> ContextClip.ADJUST_HELMET;case CHEST -> ContextClip.ADJUST_CHEST;
            case LEGS -> ContextClip.ADJUST_LEGS;case FEET -> ContextClip.ADJUST_BOOTS;default -> null;
        };
        if(clip!=null)queue(p,clip);
    }
    @SubscribeEvent public static void craft(PlayerEvent.ItemCraftedEvent e){if(e.getEntity() instanceof ServerPlayer p&&!e.getCrafting().isEmpty())queue(p,ContextClip.CRAFT);}
    @SubscribeEvent public static void anvil(AnvilCraftEvent.Post e){if(e.getEntity() instanceof ServerPlayer p&&!e.getOutput().isEmpty())queue(p,ContextClip.ANVIL_WORK);}
    @SubscribeEvent public static void enchant(PlayerEnchantItemEvent e){if(e.getEntity() instanceof ServerPlayer p&&!e.getEnchantments().isEmpty())queue(p,ContextClip.ENCHANT_WORK);}
    @SubscribeEvent public static void brew(PlayerBrewedPotionEvent e){if(e.getEntity() instanceof ServerPlayer p&&!e.getStack().isEmpty())queue(p,ContextClip.BREW_WORK);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){STATES.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){STATES.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){STATES.remove(e.getOriginal().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){STATES.clear();}
    static void resetAfterTeleport(ServerPlayer p) { STATES.remove(p.getUUID()); }
}
