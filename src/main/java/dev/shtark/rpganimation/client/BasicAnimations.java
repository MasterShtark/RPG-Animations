package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.*;
import dev.shtark.rpganimation.animation.*;
import dev.shtark.rpganimation.client.compat.AnimationCompatibility;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.tags.ItemTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.renderstate.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

@EventBusSubscriber(modid=RpgAnimation.ID,value=Dist.CLIENT)
public final class BasicAnimations {
    public record Frame(BasicAction action,BasicAction.Pose pose,boolean offhand,boolean eating,double phase,double gait,boolean sprint,double blend) {
        Frame(BasicAction a,BasicAction.Pose p,boolean o,boolean e,double phase,double gait,boolean sprint){this(a,p,o,e,phase,gait,sprint,1);}
    }
    public static final ContextKey<Frame> FRAME=new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"basic_pose"));
    private record Run(BasicAction action,PlaybackSnapshot clock,boolean offhand,net.minecraft.world.phys.Vec3 origin) {}
    private static final ContextKey<net.minecraft.world.phys.Vec3> OFFSET=new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"mount_offset"));
    private static final Map<UUID,Run> ACTIVE=new HashMap<>();
    private record GroundBlend(double previous,double current) {}
    private static final Map<UUID,GroundBlend> GROUND=new HashMap<>();
    private record ContinuousRun(BasicAction action,long start) {}
    private record Fade(BasicAction action,BasicAction.Pose pose,boolean offhand,long start) {}
    private static final Map<UUID,ContinuousRun> CONTINUOUS=new HashMap<>();
    private static final Map<UUID,Fade> FADES=new HashMap<>();
    private record SwingRun(Object nativeSwing,BasicAction action,boolean offhand,long start,BasicAction.Pose origin) {}
    private static final Map<UUID,SwingRun> SWINGS=new HashMap<>();
    private static BasicAction.Pose swingPose(SwingRun run,double now) {
        var target=run.action.sample((now-run.start)/run.action.duration);
        if(run.origin==null)return target;
        double t=MotionCurve.ease((now-run.start)/3),u=1-t;var a=run.origin;
        return new BasicAction.Pose(a.arm()*u+target.arm()*t,a.yaw()*u+target.yaw()*t,
                a.roll()*u+target.roll()*t,a.support()*u+target.support()*t,a.lean()*u+target.lean()*t,
                0,0,a.weight()*u+target.weight()*t);
    }
    private static long pageAfter;
    public static void pageChanged(){if(ticks<pageAfter||!ClientConfig.BASIC.get(BasicAction.PAGE).get())return;
        pageAfter=ticks+10;net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new PageTurnRequest());}
    private static long ticks;
    private static Object dimension;
    private static void checkDimension() {
        var level=Minecraft.getInstance().level;var current=level==null?null:level.dimension();
        if(!Objects.equals(current,dimension)){ACTIVE.clear();GROUND.clear();CONTINUOUS.clear();FADES.clear();SWINGS.clear();dimension=current;}
    }
    public static void accept(BasicActionPayload p) {
        checkDimension();
        if(!p.active()||!Double.isFinite(p.x())||!Double.isFinite(p.y())||!Double.isFinite(p.z())||p.kind()<0||p.kind()>=BasicAction.values().length){ACTIVE.remove(p.player());return;}
        var action=BasicAction.values()[p.kind()];
        if(p.elapsed()<0||p.elapsed()>=action.duration){ACTIVE.remove(p.player());return;}
        ACTIVE.put(p.player(),new Run(action,new PlaybackSnapshot(p.elapsed(),ticks),p.offhand(),new net.minecraft.world.phys.Vec3(p.x(),p.y(),p.z())));
    }
    private static boolean eligible(Player p) {
        return !ClientConfig.REDUCED_MOTION.get()&&ServerConfig.BASIC_ACTIONS.get()&&p.isAlive()&&!p.isSpectator()
                &&!BedAnimations.isRunning(p.getUUID())&&ObserverAnimations.time(p.getUUID(),0)==null
                &&DropAnimations.time(p.getUUID(),0)==null&&PickupAnimations.time(p.getUUID(),0)==null;
    }
    private static Frame frame(Player p,float partial,double walk,double speed) {
        if(!eligible(p))return null;
        var run=ACTIVE.get(p.getUUID());
        boolean landing=run!=null&&(run.action==BasicAction.LAND_HARD||run.action==BasicAction.LAND_SOFT||run.action==BasicAction.ELYTRA_LAND);
        if(p.hurtTime>0&&!landing&&(run==null||run.action!=BasicAction.SHIELD_HIT))return null;
        if(p.isSleeping()&&(run==null||run.action!=BasicAction.SLEEP_SETTLE))return null;
        if(run!=null&&ClientConfig.BASIC.get(run.action).get()&&(!run.action.mount()||p.isPassenger())
                &&(!run.action.dismount()||!p.isPassenger())&&(run.action!=BasicAction.SLEEP_SETTLE||p.isSleeping())) {
            double time=run.clock.time(ticks,partial)/run.action.duration;
            if(time<1)return new Frame(run.action,run.action.sample(time),run.offhand,false,time,0,false);
        }
        if(p.isSleeping())return null;
        if(p.isFallFlying()&&!ClientConfig.BASIC.get(BasicAction.ELYTRA_FLIGHT).get())return null;
        var continuous=continuous(p);
        if(continuous!=null&&ClientConfig.BASIC.get(continuous).get()) {
            var current=CONTINUOUS.get(p.getUUID());
            double blend=MotionCurve.ease((current==null?0:ticks+partial-current.start)/5);
            double phase=continuousPhase(p,continuous,partial);
            return new Frame(continuous,continuous.continuousPose(phase,blend),continuousOffhand(p,continuous),false,phase,0,false);
        }
        if(p.isUsingItem()) {
            if(!ClientConfig.EATING.get()||p.getUseItem().getUseAnimation()!=ItemUseAnimation.EAT)return null;
            double age=p.getTicksUsingItem(partial),blend=MotionCurve.ease(age/4);
            double bite=.5-.5*Math.cos(age*Math.PI/2.5);
            var pose=new BasicAction.Pose((-1.9-.14*bite)*blend,.12*blend,.12*blend,-.12*blend,.025*bite*blend,0,0,blend);
            return new Frame(null,pose,p.getUsedItemHand()==InteractionHand.OFF_HAND,true,age,0,false);
        }
        var fade=FADES.get(p.getUUID());
        if(fade!=null&&ClientConfig.BASIC.get(fade.action).get()) {
            double factor=1-MotionCurve.ease((ticks+partial-fade.start)/5);
            if(factor>0)return new Frame(fade.action,scaled(fade.pose,factor),fade.offhand,false,0,0,false);
        }
        if(p.isPassenger())return null;
        var cosmeticSwing=SWINGS.get(p.getUUID());
        if(cosmeticSwing!=null&&ClientConfig.BASIC.get(cosmeticSwing.action).get()) {
            double phase=(ticks+partial-cosmeticSwing.start)/cosmeticSwing.action.duration;
            return new Frame(cosmeticSwing.action,swingPose(cosmeticSwing,ticks+partial),cosmeticSwing.offhand,false,phase,0,false);
        }
        if(p.isSwinging()) {
            var swing=p.getCurrentSwing();boolean off=swing!=null&&swing.hand()==InteractionHand.OFF_HAND;
            var item=p.getItemInHand(off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
            BasicAction action=item.is(ItemTags.AXES)?BasicAction.CHOP:item.is(ItemTags.SHOVELS)?BasicAction.DIG:
                    item.is(ItemTags.PICKAXES)?BasicAction.PICK:BasicAction.SWING;
            // Before the next client tick captures this swing, hold the neutral
            // replacement instead of briefly sampling a different native timeline.
            if(ClientConfig.BASIC.get(action).get())return new Frame(action,action.sample(0),off,false,0,0,false);
        }
        boolean sprint=p.isSprinting();
        var ground=GROUND.get(p.getUUID());
        double grounded=ground==null?(p.onGround()?1:0):ground.previous+(ground.current-ground.previous)*partial;
        if(grounded>0&&!p.isInWater()&&!p.isFallFlying()&&!p.isCrouching()&&speed>.01&&(sprint?ClientConfig.RUNNING.get():ClientConfig.WALKING.get()))
            return new Frame(null,null,false,false,walk,Math.min(1,speed),sprint,grounded);
        return null;
    }
    private static BasicAction continuous(Player p) {
        if(p.isSleeping()||p.isPassenger())return null;
        if(p.isFallFlying())return BasicAction.ELYTRA_FLIGHT;
        if(p.isUsingItem())return switch(p.getUseItem().getUseAnimation()) {
            case BLOCK -> BasicAction.SHIELD;case BOW -> BasicAction.BOW_DRAW;case CROSSBOW -> BasicAction.CROSSBOW_LOAD;default -> null;
        };
        if(p.onClimbable())return BasicAction.CLIMB;
        if(p.isSwimming())return BasicAction.SWIM_LOOP;
        for(var hand:InteractionHand.values())if(p.getItemInHand(hand).getUseAnimation()==ItemUseAnimation.CROSSBOW&&net.minecraft.world.item.CrossbowItem.isCharged(p.getItemInHand(hand)))return BasicAction.CROSSBOW_AIM;
        return null;
    }
    private static boolean continuousOffhand(Player p,BasicAction action) {
        return action==BasicAction.CROSSBOW_AIM?!(p.getMainHandItem().getUseAnimation()==ItemUseAnimation.CROSSBOW&&net.minecraft.world.item.CrossbowItem.isCharged(p.getMainHandItem())):p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND;
    }
    private static double continuousPhase(Player p,BasicAction action,float partial) {
        return action==BasicAction.CLIMB?p.getPosition(partial).y*12:(action==BasicAction.SWIM_LOOP||action==BasicAction.ELYTRA_FLIGHT)?p.level().getGameTime()+partial:p.getTicksUsingItem(partial);
    }
    private static BasicAction.Pose scaled(BasicAction.Pose p,double w) {return new BasicAction.Pose(p.arm()*w,p.yaw()*w,p.roll()*w,p.support()*w,p.lean()*w,p.leg()*w,p.otherLeg()*w,p.weight()*w);}
    private static void continuousTick() {
        var level=Minecraft.getInstance().level;if(level==null)return;
        var present=new HashSet<UUID>();
        for(var p:level.players()) {
            var id=p.getUUID();present.add(id);
            var swing=p.getCurrentSwing();var previous=SWINGS.get(id);
            if(eligible(p)&&swing!=null&&(previous==null||previous.nativeSwing!=swing)) {
                boolean off=swing.hand()==InteractionHand.OFF_HAND;
                var item=p.getItemInHand(off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                BasicAction action=item.is(ItemTags.AXES)?BasicAction.CHOP:item.is(ItemTags.SHOVELS)?BasicAction.DIG:
                        item.is(ItemTags.PICKAXES)?BasicAction.PICK:BasicAction.SWING;
                boolean continuing=previous!=null&&ticks-previous.start<previous.action.duration;
                if(continuing&&previous.action==action&&previous.offhand==off) {
                    // Rapid native swings must not continually reset a slower cosmetic cycle.
                    SWINGS.put(id,new SwingRun(swing,action,off,previous.start,previous.origin));
                } else {
                    var origin=continuing?swingPose(previous,ticks):null;
                    SWINGS.put(id,new SwingRun(swing,action,off,ticks,origin));
                }
            } else if(!eligible(p)||(swing==null&&previous!=null&&ticks-previous.start>=previous.action.duration))SWINGS.remove(id);
            var ground=GROUND.get(id);double oldGround=ground==null?(p.onGround()?1:0):ground.current;
            GROUND.put(id,new GroundBlend(oldGround,Math.clamp(oldGround+(p.onGround()?.2:-.2),0,1)));
            var next=eligible(p)?continuous(p):null;
            var old=CONTINUOUS.get(id);
            if(next!=null&&ClientConfig.BASIC.get(next).get()) {
                if(old==null||old.action!=next)CONTINUOUS.put(id,new ContinuousRun(next,ticks));
                FADES.put(id,new Fade(next,next.continuousPose(continuousPhase(p,next,0),MotionCurve.ease(old==null?0:(ticks-old.start)/5.0)),continuousOffhand(p,next),ticks));
            } else {CONTINUOUS.remove(id);}
        }
        SWINGS.keySet().retainAll(present);
        GROUND.keySet().retainAll(present);CONTINUOUS.keySet().retainAll(present);FADES.entrySet().removeIf(e->!present.contains(e.getKey())||ticks-e.getValue().start>=5);
    }
    private static net.minecraft.world.phys.Vec3 mountOffset(Player p,float partial,net.minecraft.world.phys.Vec3 position) {
        var run=ACTIVE.get(p.getUUID());
        if(run==null||(!run.action.mount()&&!run.action.dismount())||!eligible(p)||(run.action.mount()&&!p.isPassenger())||(run.action.dismount()&&p.isPassenger())||!ClientConfig.BASIC.get(run.action).get())return net.minecraft.world.phys.Vec3.ZERO;
        double t=run.clock.time(ticks,partial)/run.action.duration;
        var delta=run.origin.subtract(position);
        if(delta.lengthSqr()>16)return net.minecraft.world.phys.Vec3.ZERO;
        return delta.scale(1-MotionCurve.ease(t/.85));
    }
    public static void mountTransform(AvatarRenderState state,PoseStack stack,float scale) {
        if(!AnimationCompatibility.bodyAllowed(state))return;
        var offset=state.getRenderData(OFFSET);
        if(offset!=null&&scale>0)stack.translate(offset.x/scale,offset.y/scale,offset.z/scale);
    }
    public static net.minecraft.world.phys.Vec3 cameraOffset(float partial) {
        var p=Minecraft.getInstance().player;
        return p==null?net.minecraft.world.phys.Vec3.ZERO:mountOffset(p,partial,p.getPosition(partial));
    }
    public static boolean mounting(UUID player) {var r=ACTIVE.get(player);return r!=null&&(r.action.mount()||r.action.dismount());}
    @SubscribeEvent public static void extraHand(RenderHandEvent event) {
        if(!AnimationCompatibility.localPresentationAllowed())return;
        var state=event.getPlayerState().avatarRenderState;
        if(state==null||state.isInvisible)return;var f=state.getRenderData(FRAME);
        if(f==null||f.pose==null||f.action==BasicAction.SWIM_LOOP||Math.abs(f.pose.support())<.05)return;
        var activeHand=f.offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        if(event.getHand()!=activeHand)return;
        var supportHand=f.offhand?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        var mc=Minecraft.getInstance();if(mc.player==null||!mc.player.getItemInHand(supportHand).isEmpty())return;
        var renderer=(net.minecraft.client.renderer.entity.player.AvatarRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(state);
        boolean right=(state.mainArm==HumanoidArm.LEFT)!=f.offhand;float side=right?1:-1;var stack=event.getPoseStack();stack.pushPose();
        try {
            firstPerson(state,supportHand,stack);
            stack.translate(side*.48,-1.25,-.65);stack.rotateDegrees(Axis.XP,-25);stack.translate(side*.3125,-.125,0);
            if(right)renderer.renderRightHand(stack,event.getSubmitNodeCollector(),event.getPackedLight(),state.skin.body().texturePath(),state.showRightSleeve);
            else renderer.renderLeftHand(stack,event.getSubmitNodeCollector(),event.getPackedLight(),state.skin.body().texturePath(),state.showLeftSleeve);
        } finally {stack.popPose();}
    }
    /** Swim support hands cannot depend on a normal hand event surviving other renderers. */
    public static void swimmingSupport(net.minecraft.client.renderer.state.level.PlayerRenderState player,
            PoseStack stack, net.minecraft.client.renderer.SubmitNodeCollector collector) {
        if (!AnimationCompatibility.localPresentationAllowed("swim_loop")) return;
        var state=player.avatarRenderState;
        if(state==null||state.isInvisible)return;
        var f=state.getRenderData(FRAME);
        var mc=Minecraft.getInstance();
        if(f==null||f.action!=BasicAction.SWIM_LOOP||mc.player==null||!mc.player.getOffhandItem().isEmpty())return;
        var renderer=(net.minecraft.client.renderer.entity.player.AvatarRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(state);
        boolean right=state.mainArm==HumanoidArm.LEFT;float side=right?1:-1;
        stack.pushPose();
        try {
            firstPerson(state,InteractionHand.OFF_HAND,stack);
            stack.translate(side*.48,-1.25,-.65);
            stack.rotateDegrees(Axis.XP,-25);stack.translate(side*.3125,-.125,0);
            if(right)renderer.renderRightHand(stack,collector,state.lightCoords,state.skin.body().texturePath(),state.showRightSleeve);
            else renderer.renderLeftHand(stack,collector,state.lightCoords,state.skin.body().texturePath(),state.showLeftSleeve);
        } finally {stack.popPose();}
    }

    public static String actionKey(Frame frame) {
        if(frame==null)return null;
        return frame.action!=null?frame.action.name().toLowerCase(java.util.Locale.ROOT):frame.eating?"eating":frame.sprint?"running":"walking";
    }
    public static String localActionKey() {
        var p=Minecraft.getInstance().player;
        return p==null?null:actionKey(frame(p,0,p.walkAnimation.position(0),p.walkAnimation.speed(0)));
    }
    public static boolean localActive() {
        var p=Minecraft.getInstance().player;
        return p!=null&&AnimationCompatibility.localPresentationAllowed()&&frame(p,0,0,0)!=null;
    }
    private static boolean renderedSwing;
    public static void resetHandPass(){renderedSwing=false;}
    public static boolean replaceSwing() { return renderedSwing; }
    public static boolean replaceBodySwing(AvatarRenderState state) {
        var f=state.getRenderData(FRAME);
        return f!=null&&f.action!=null&&f.action.ordinal()<=BasicAction.PICK.ordinal()
                &&AnimationCompatibility.bodyAllowed(state);
    }
    public static boolean replaceEating() {
        var p=Minecraft.getInstance().player;if(p==null||!AnimationCompatibility.localPresentationAllowed())return false;
        var f=frame(p,0,0,0);return f!=null&&f.eating;
    }
    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier(){
            @Override public <T extends Avatar & ClientAvatarEntity> void accept(T avatar,AvatarRenderState state) {
                state.setRenderData(FRAME,avatar instanceof Player p?frame(p,state.partialTick,state.walkAnimationPos,state.walkAnimationSpeed):null);
                state.setRenderData(OFFSET,avatar instanceof Player p?mountOffset(p,state.partialTick,new net.minecraft.world.phys.Vec3(state.x,state.y,state.z)):net.minecraft.world.phys.Vec3.ZERO);
            }
        });
    }
    public static boolean model(HumanoidModel<?> model,AvatarRenderState state) {
        var f=state.getRenderData(FRAME);if(f==null)return false;
        if(f.pose==null&&state.getRenderData(ContextAnimations.FRAME)!=null)return false;
        if(f.pose==null) {
            // Keep vanilla leg phase, with opposing shoulders and a small footfall weight shift.
            double phase=f.phase*.6662, speed=f.gait, weight=MotionCurve.ease(f.blend), lean=f.sprint?.065:.02;
            model.body.xRot+=(float)(lean*speed*weight);
            model.body.zRot+=(float)(Math.sin(phase)*speed*weight*(f.sprint?.035:.02));
            model.rightArm.xRot=(float)(model.rightArm.xRot*(1-weight)+Math.cos(phase+Math.PI)*speed*(f.sprint?.9:.65)*weight);
            model.leftArm.xRot=(float)(model.leftArm.xRot*(1-weight)+Math.cos(phase)*speed*(f.sprint?.9:.65)*weight);
            model.rightArm.zRot=(float)(model.rightArm.zRot*(1-weight)-.07*speed*weight);model.leftArm.zRot=(float)(model.leftArm.zRot*(1-weight)+.07*speed*weight);
            return true;
        }
        var pose=f.pose;boolean right=(state.mainArm==HumanoidArm.RIGHT)!=f.offhand;
        var arm=right?model.rightArm:model.leftArm;var support=right?model.leftArm:model.rightArm;
        float weight=(float)pose.weight(),sign=right?1:-1;
        if(f.action==BasicAction.SHIELD_HIT){arm.xRot+=(float)pose.arm();arm.yRot+=(float)pose.yaw()*sign;model.body.xRot+=(float)pose.lean();return true;}
        arm.xRot=arm.xRot*(1-weight)+(float)pose.arm();arm.yRot=arm.yRot*(1-weight)+(float)pose.yaw()*sign;
        arm.zRot=arm.zRot*(1-weight)+(float)pose.roll()*sign;
        support.xRot=support.xRot*(1-weight)+(float)pose.support();
        if(f.action==BasicAction.SWIM_LOOP||f.action==BasicAction.ELYTRA_FLIGHT) {
            // Vanilla's swimming pose also rotates the support arm about Y and Z.
            support.yRot*=1-weight;
            support.zRot=support.zRot*(1-weight)-(float)pose.roll()*sign;
        }
        model.body.xRot+=(float)pose.lean();
        if(f.action==BasicAction.SLEEP_SETTLE){model.head.xRot+=(float)pose.lean()*3;model.head.zRot+=(float)pose.roll();}
        if(f.action!=null&&(f.action.continuous()||f.action==BasicAction.LAND_SOFT||f.action==BasicAction.LAND_HARD||f.action==BasicAction.SWIM_EXIT||f.action==BasicAction.ELYTRA_TAKEOFF||f.action==BasicAction.ELYTRA_LAND)) {
            if(f.action==BasicAction.CLIMB||f.action==BasicAction.SWIM_LOOP||f.action==BasicAction.LAND_SOFT||f.action==BasicAction.LAND_HARD||f.action==BasicAction.SWIM_EXIT||f.action==BasicAction.ELYTRA_TAKEOFF||f.action==BasicAction.ELYTRA_LAND) {
                model.rightLeg.xRot=model.rightLeg.xRot*(1-weight)+(float)pose.leg();model.leftLeg.xRot=model.leftLeg.xRot*(1-weight)+(float)pose.otherLeg();
            }
            if(f.action==BasicAction.LAND_SOFT||f.action==BasicAction.LAND_HARD)model.root().y-=(float)pose.leg()*2;
        }
        if(f.action!=null&&f.action.dismount()) {
            float seated=(float)(1-MotionCurve.ease(f.phase/.7));
            model.rightLeg.xRot=(float)pose.leg()-1.25f*seated;model.leftLeg.xRot=(float)pose.otherLeg()-1.25f*seated;
            float transfer=(float)BasicAction.pulse(f.phase,0,.32,.8);
            model.rightLeg.yRot=.35f*transfer;model.rightLeg.zRot=.12f*transfer;
            model.leftLeg.yRot=-.1f*transfer;
        }
        if(f.action!=null&&f.action.mount()) {
            float seated=(float)MotionCurve.ease((f.phase-.6)/.4);
            model.rightLeg.xRot=model.rightLeg.xRot*seated+(float)pose.leg();
            model.leftLeg.xRot=model.leftLeg.xRot*seated+(float)pose.otherLeg();
            model.rightLeg.yRot*=seated;model.leftLeg.yRot*=seated;
            model.rightLeg.zRot*=seated;model.leftLeg.zRot*=seated;
        }
        return true;
    }
    public static boolean ownsAttackHeight(Player player) {
        if(!eligible(player)||player.isUsingItem()||player.isSleeping()||player.isPassenger()
                ||ContextAnimations.nativeRodHand(InteractionHand.MAIN_HAND))return false;
        var item=player.getMainHandItem();
        if(item.isEmpty())return false;
        BasicAction action=item.is(ItemTags.AXES)?BasicAction.CHOP:item.is(ItemTags.SHOVELS)?BasicAction.DIG:
                item.is(ItemTags.PICKAXES)?BasicAction.PICK:BasicAction.SWING;
        return ClientConfig.BASIC.get(action).get()
                &&AnimationCompatibility.localPresentationAllowed(action.name().toLowerCase(Locale.ROOT));
    }
    public static boolean firstPerson(AvatarRenderState state,InteractionHand hand,PoseStack stack) {
        if(state==null||!AnimationCompatibility.localPresentationAllowed())return false;
        var f=state.getRenderData(FRAME);if(f==null)return false;
        boolean main=(hand==InteractionHand.OFF_HAND)==f.offhand;
        double side=(hand==InteractionHand.MAIN_HAND?state.mainArm:state.mainArm.getOpposite())==HumanoidArm.RIGHT?1:-1;
        if(f.pose==null&&state.getRenderData(ContextAnimations.FRAME)!=null)return false;
        if(f.pose==null) {
            stack.translate(0,Math.sin(f.phase*1.3324)*f.gait*.012,0);return true;
        }
        var p=f.pose;
        // Preserve vanilla aiming and projectile alignment; animate the assisting hand.
        if(main&&(f.action==BasicAction.BOW_DRAW||f.action==BasicAction.CROSSBOW_LOAD||f.action==BasicAction.CROSSBOW_AIM||f.action==BasicAction.SHIELD))return true;
        double x=main?p.arm():p.support();
        boolean attack=main&&f.action!=null&&f.action.ordinal()<=BasicAction.PICK.ordinal();
        if(attack) {
            // A bare fist has a different native arm pivot and no tool wrist.
            // Keep its native punch instead of applying a held-tool transform.
            var player=Minecraft.getInstance().player;
            if(player==null||player.getItemInHand(hand).isEmpty())return false;
            // Preserve vanilla's grip position. Project the observer's arm rotation
            // into camera coordinates, with a smaller angular range for the near view.
            // There is no alternate arm model, forward offset or weight-based orbit.
            stack.translate(side*.56,-.52,-.72);
            stack.rotateDegrees(Axis.ZP,(float)(p.roll()*side*24));
            stack.rotateDegrees(Axis.YP,(float)(-p.yaw()*side*24));
            // The observer raises the elbow for wind-up, then extends into the
            // contact beat. A held first-person tool needs wrist extension on
            // that same beat, rather than treating elbow lift as wrist pitch.
            double contact=BasicAction.pulse(f.phase,.16,.42,.75);
            stack.rotateDegrees(Axis.XP,(float)(-p.arm()*12-65*contact));
            stack.translate(-side*.56,.52,.72);
            renderedSwing=true;
            return true;
        }
        stack.translate(-side*.06*Math.abs(x),-.10*p.lean()-.09*Math.sin(x),-.14*Math.sin(-x));
        stack.rotateDegrees(Axis.XP,(float)(x*30));
        if(main){stack.rotateDegrees(Axis.YP,(float)(p.yaw()*35*side));stack.rotateDegrees(Axis.ZP,(float)(p.roll()*40*side));}
        return true;
    }
    @SubscribeEvent public static void sleepCamera(ViewportEvent.ComputeCameraAngles e) {
        var mc=Minecraft.getInstance();if(mc.player==null||!mc.options.getCameraType().isFirstPerson()||!AnimationCompatibility.localPresentationAllowed())return;
        var f=frame(mc.player,(float)e.getPartialTick(),0,0);
        if(f!=null&&f.action==BasicAction.SLEEP_SETTLE){e.setRoll(e.getRoll()+(float)(f.pose.lean()*35));e.setPitch(e.getPitch()+(float)(f.pose.lean()*20));}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){checkDimension();if(!Minecraft.getInstance().isPaused())ticks++;
        ACTIVE.values().removeIf(r->r.clock.time(ticks,0)>=r.action.duration);continuousTick();}
    @SubscribeEvent public static void clone(ClientPlayerNetworkEvent.Clone e){ACTIVE.remove(e.getOldPlayer().getUUID());GROUND.remove(e.getOldPlayer().getUUID());CONTINUOUS.remove(e.getOldPlayer().getUUID());FADES.remove(e.getOldPlayer().getUUID());SWINGS.remove(e.getOldPlayer().getUUID());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){ACTIVE.clear();GROUND.clear();CONTINUOUS.clear();FADES.clear();SWINGS.clear();dimension=null;ticks=0;pageAfter=0;}
}
