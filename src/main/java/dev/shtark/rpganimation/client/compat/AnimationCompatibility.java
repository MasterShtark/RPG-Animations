package dev.shtark.rpganimation.client.compat;

import dev.shtark.rpganimation.RpgAnimation;
import dev.shtark.rpganimation.animation.*;
import dev.shtark.rpganimation.client.*;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;

/** Per-action ownership. Optional Better Combat classes are never statically linked. */
@net.neoforged.fml.common.EventBusSubscriber(modid=RpgAnimation.ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class AnimationCompatibility {
    private static final FrameCache<Object,Boolean> RAGDOLL=new FrameCache<>();
    private static final FrameCache<Object,Integer> COMBAT = new FrameCache<>();
    private static final FrameCache<Object,String> LOCAL_ACTION = new FrameCache<>();
    private static final FrameCache<Object,Boolean> EXTRA_OWNER = new FrameCache<>();
    private static final Object OWNER_KEY = new Object();
    @net.neoforged.bus.api.SubscribeEvent
    public static void beginFrame(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
        RAGDOLL.begin(); RagdollCompatibility.clearFrame(); COMBAT.begin(); LOCAL_ACTION.begin(); EXTRA_OWNER.begin();
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void endFrame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        RAGDOLL.end(); RagdollCompatibility.clearFrame(); COMBAT.end(); LOCAL_ACTION.end(); EXTRA_OWNER.end();
    }
    private static final ContextKey<Boolean> RAGDOLL_STATE=new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"ragdoll_owner"));
    private static final ContextKey<Integer> COMBAT_STATE=new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"combat_owner_state"));
    private static boolean initialized,combat,punchy,unavailable,warned;
    private static final Identifier ATTACK_LAYER=Identifier.fromNamespaceAndPath("bettercombat","attack");
    private static Method layer,activeLayer,swing;
    private AnimationCompatibility() {}
    private static void initialize(){
        if(initialized)return;initialized=true;
        combat=ModList.get().isLoaded("bettercombat");punchy=ModList.get().isLoaded("punchy");
        if(!combat)return;
        try{
            layer=Class.forName("com.zigythebird.playeranim.api.PlayerAnimationAccess").getMethod("getPlayerAnimationLayer",Avatar.class,Identifier.class);
            activeLayer=Class.forName("net.bettercombat.client.animation.AttackAnimationStack").getMethod("isActive");
            swing=Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat").getMethod("isWeaponSwingInProgress");
        }catch(ReflectiveOperationException|LinkageError e){fail(e);}
    }
    private static void fail(Throwable e){
        unavailable=true;if(warned)return;warned=true;
        com.mojang.logging.LogUtils.getLogger().warn("RPG Animation cannot inspect Better Combat's attack API. Automatic priority will yield combat actions only; unrelated RPG actions remain enabled. Check the installed version or use per-action overrides.",e);
    }
    /** 0 = no attack, 1 = attack, 2 = API unavailable. Idle weapon poses are not attacks. */
    private static int combatState(Object player){
        return COMBAT.get(player,AnimationCompatibility::readCombatState);
    }
    private static int readCombatState(Object player){
        initialize();if(!combat||!ClientConfig.BETTER_COMBAT_HANDOFF.get()||player==null)return 0;
        if(unavailable)return 2;
        try{
            // Read the playing layer, not getCurrentAttack (which also resolves idle weapons).
            Object animation=layer.invoke(null,player,ATTACK_LAYER);
            if(animation==null)return 2;
            boolean active=Boolean.TRUE.equals(activeLayer.invoke(animation));
            if(player==Minecraft.getInstance().player)active|=Boolean.TRUE.equals(swing.invoke(Minecraft.getInstance()));
            return active?1:0;
        }catch(ReflectiveOperationException|LinkageError|IllegalArgumentException e){fail(e);return 2;}
    }
    private static boolean allowed(String key,boolean firstPerson,int state){
        initialize();
        if(EXTRA_OWNER.get(OWNER_KEY,ignored -> ClientConfig.ADDITIONAL_ANIMATION_OWNERS.get().stream().anyMatch(id->ModList.get().isLoaded(id))))return false;
        var setting=ClientConfig.ACTION_OWNERS.get(key);
        var preference=setting==null?AnimationPreference.AUTO:setting.get();
        if(preference==AnimationPreference.AUTO&&AnimationChannels.inspectionAction(key)&&!key.equals("inspect")) {
            var parent=ClientConfig.ACTION_OWNERS.get("inspect");
            if(parent!=null)preference=parent.get();
        }
        if(preference==AnimationPreference.AUTO&&state==1&&combat&&ClientConfig.BETTER_COMBAT_HANDOFF.get()
                &&AnimationChannels.detailAction(key))return false;
        return AnimationOwnership.allowed(preference,firstPerson,punchy&&ClientConfig.PUNCHY_HANDOFF.get(),(AnimationChannels.PUNCHY.contains(key)||AnimationChannels.inspectionAction(key)),
                combat&&ClientConfig.BETTER_COMBAT_HANDOFF.get(),state==1,state==2,AnimationChannels.combatAction(key));
    }
    private static boolean externalPose(Object player){return RagdollCompatibility.owns(player)||OptionalPoseCompatibility.owns(player);}
    public static boolean externalPoseOwned(AvatarRenderState state){return Boolean.TRUE.equals(state.getRenderData(RAGDOLL_STATE));}
    public static boolean localPresentationAllowed(String action){return !RAGDOLL.get(Minecraft.getInstance().player,AnimationCompatibility::externalPose)&&allowed(action,true,combatState(Minecraft.getInstance().player));}
    public static boolean localPresentationAllowed(){return localPresentationAllowed(LOCAL_ACTION.get(Minecraft.getInstance().player,ignored -> localAction()));}
    private static String localAction(){
        if(IntroController.active())return "wake_up";
        String bed=BedAnimations.localActionKey();if(bed!=null)return bed;
        if(DropAnimations.localActive())return "drop";
        String basic=BasicAnimations.localActionKey(),context=ContextAnimations.localActionKey();
        if(basic!=null&&!basic.equals("walking")&&!basic.equals("running"))return basic;
        if(context!=null)return context;
        if(PickupAnimations.localActive())return "pickup";
        if(basic!=null)return basic;
        if(ActivityAnimations.typingHands())return "activity_typing";
        if(ContextAnimations.fishingHands())return "fishing_hold";
        return "idle";
    }
    public static boolean bodyAllowed(AvatarRenderState state){return bodyAllowed(state,action(state));}
    public static boolean bodyAllowed(AvatarRenderState state,String action){
        if(Boolean.TRUE.equals(state.getRenderData(RAGDOLL_STATE)))return false;
        Integer combat=state.getRenderData(COMBAT_STATE);return allowed(action,false,combat==null?0:combat);
    }
    private static String action(AvatarRenderState state){
        String bed=state.getRenderData(BedAnimations.ACTION);if(bed!=null)return bed;
        if(state.getRenderData(ObserverAnimations.TIME)!=null)return "wake_up";
        if(state.getRenderData(DropAnimations.TIME)!=null)return "drop";
        var basic=state.getRenderData(BasicAnimations.FRAME);var context=state.getRenderData(ContextAnimations.FRAME);
        if(basic!=null&&(basic.pose()!=null||context==null))return BasicAnimations.actionKey(basic);
        if(context!=null)return context.clip().name().toLowerCase(java.util.Locale.ROOT);
        if(state.getRenderData(PickupAnimations.TIME)!=null)return "pickup";
        String activity=ActivityAnimations.actionKey(state);return activity==null?"idle":activity;
    }
    public static void register(RegisterRenderStateModifiersEvent event){
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier(){
            @Override public <T extends Avatar & ClientAvatarEntity> void accept(T avatar,AvatarRenderState state){state.setRenderData(COMBAT_STATE,combatState(avatar));state.setRenderData(RAGDOLL_STATE,RAGDOLL.get(avatar,AnimationCompatibility::externalPose));}
        });
    }
}
