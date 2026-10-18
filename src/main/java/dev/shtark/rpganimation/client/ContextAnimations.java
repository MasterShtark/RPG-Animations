package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.*;
import dev.shtark.rpganimation.animation.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.InteractionHand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.renderstate.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid=RpgAnimation.ID,value=Dist.CLIENT)
public final class ContextAnimations {
    public record Frame(ContextClip clip,int variant,ContextClip.Pose pose,double seatLeg) {}
    public static final ContextKey<Frame> FRAME=new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"context_pose"));
    private record Run(ContextClip clip,int variant,PlaybackSnapshot clock,net.minecraft.world.phys.Vec3 origin) {}
    private static final ContextKey<net.minecraft.world.phys.Vec3> OFFSET=new ContextKey<>(Identifier.fromNamespaceAndPath(RpgAnimation.ID,"seat_offset"));
    private static final Map<UUID,Run> ACTIVE=new HashMap<>();
    private record Transition(Frame origin,long start,net.minecraft.world.phys.Vec3 offset) {}
    private static final Map<UUID,net.minecraft.world.phys.Vec3> SEAT_OFFSETS=new HashMap<>();
    private static final Map<UUID,Transition> TRANSITIONS=new HashMap<>();
    private static long ticks,requestAfter;
    private static Object dimension;
    private static void checkDimension(){var l=Minecraft.getInstance().level;var d=l==null?null:l.dimension();
        if(!Objects.equals(d,dimension)){ACTIVE.clear();TRANSITIONS.clear();SEAT_OFFSETS.clear();dimension=d;}}
    public static void accept(ContextPayload p) {
        checkDimension();
        var previous=frame(p.player(),0);
        if(previous!=null)TRANSITIONS.put(p.player(),new Transition(previous,ticks,SEAT_OFFSETS.getOrDefault(p.player(),net.minecraft.world.phys.Vec3.ZERO)));
        if(!p.active()||p.kind()<0||p.kind()>=ContextClip.values().length||p.variant()<0||p.variant()>3){ACTIVE.remove(p.player());return;}
        var clip=ContextClip.values()[p.kind()];
        if(!Double.isFinite(p.x()+p.y()+p.z())||p.elapsed()<0||p.elapsed()>=clip.duration){ACTIVE.remove(p.player());return;}
        ACTIVE.put(p.player(),new Run(clip,p.variant(),new PlaybackSnapshot(p.elapsed(),ticks),new net.minecraft.world.phys.Vec3(p.x(),p.y(),p.z())));
    }
    private static int transitionTicks(Transition transition) {
        var clip=transition.origin.clip;
        return clip.detail()||clip.stretch()||clip.environmental()||clip==ContextClip.WARM||clip.inspection()?12:6;
    }
    private static Frame frame(UUID id,double partial) {
        if(ClientConfig.REDUCED_MOTION.get())return null;
        var run=ACTIVE.get(id);Frame target=null;
        if(run!=null&&ClientConfig.CONTEXT.get(run.clip).get()&&(!run.clip.inspection()||ClientConfig.CONTEXT.get(ContextClip.INSPECT).get())) {
            double time=run.clock.time(ticks,partial);
            if(time<run.clip.duration)target=new Frame(run.clip,run.variant,run.clip.sample(time,run.variant),seatLeg(run.clip,time));
        }
        var transition=TRANSITIONS.get(id);
        if(transition!=null&&ticks+partial-transition.start<transitionTicks(transition)&&ClientConfig.CONTEXT.get(transition.origin.clip).get()) {
            var origin=transition.origin;
            var end=target==null?origin.clip.sample(origin.clip.duration,origin.variant):target.pose;
            return new Frame(target==null?origin.clip:target.clip,target==null?origin.variant:target.variant,
                    ContextClip.blend(origin.pose,end,(ticks+partial-transition.start)/transitionTicks(transition)),
                    origin.seatLeg+( (target==null?0:target.seatLeg)-origin.seatLeg)*MotionCurve.ease((ticks+partial-transition.start)/transitionTicks(transition)));
        }
        return target;
    }

    private static double seatLeg(ContextClip clip,double time) {
        return clip.seated()?DetailActivity.seatLeg(clip==ContextClip.SEAT_ENTER,time,clip.duration):0;
    }
    public static void seatTransform(AvatarRenderState state,PoseStack pose,float scale) {
        var offset=state.getRenderData(OFFSET);
        if(offset!=null&&scale>0)pose.translate(offset.x/scale,offset.y/scale,offset.z/scale);
    }
    public static String localActionKey() {
        var p=Minecraft.getInstance().player;var f=p==null?null:frame(p.getUUID(),0);
        return f==null||!localActive()?null:f.clip.name().toLowerCase(java.util.Locale.ROOT);
    }
    public static boolean localActive() {
        var p=Minecraft.getInstance().player;
        return p!=null&&p.isAlive()&&!p.isSleeping()&&!p.isUsingItem()&&(p.hurtTime==0||(p.isOnFire()&&frame(p.getUUID(),0)!=null&&frame(p.getUUID(),0).clip==ContextClip.FIRE_PAT))
                &&!IntroController.active()&&!BedAnimations.isRunning(p.getUUID())&&!DropAnimations.localActive()
                &&frame(p.getUUID(),0)!=null;
    }
    public static boolean fishingHands() {
        var p=Minecraft.getInstance().player;
        return p!=null&&p.fishing!=null&&p.isAlive()&&!p.isSleeping()&&!ClientConfig.REDUCED_MOTION.get()
                &&(ClientConfig.CONTEXT.get(ContextClip.FISH_CAST).get()||ClientConfig.CONTEXT.get(ContextClip.FISH_WAIT).get()
                ||ClientConfig.CONTEXT.get(ContextClip.FISH_BITE).get()||ClientConfig.CONTEXT.get(ContextClip.FISH_REEL).get());
    }
    /** Match the hook renderer's main-hand-first rod selection, including modded rods. */
    public static boolean nativeRodHand(InteractionHand hand) {
        var p=Minecraft.getInstance().player;
        if(p==null||p.fishing==null)return false;
        boolean main=p.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST);
        return hand==(main?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
    }
    private static boolean right(Frame frame,AvatarRenderState state) {
        return frame.clip==ContextClip.WALL?frame.variant%2==0:
                (state.mainArm==HumanoidArm.RIGHT) != (frame.clip.fishing()&&frame.variant%2==1);
    }
    public static void model(HumanoidModel<?> model,AvatarRenderState state) {
        var f=state.getRenderData(FRAME);if(f==null)return;var p=f.pose;
        boolean right=right(f,state);var main=right?model.rightArm:model.leftArm;var other=right?model.leftArm:model.rightArm;
        float sign=right?1:-1,weight=(float)p.weight();
        main.xRot=main.xRot*(1-weight)+(float)p.mainX(); main.yRot=main.yRot*(1-weight)+(float)p.mainY()*sign;
        main.zRot=main.zRot*(1-weight)+(float)p.mainZ()*sign;
        other.xRot=other.xRot*(1-weight)+(float)p.otherX();other.yRot=other.yRot*(1-weight)+(float)p.otherY()*sign;
        other.zRot=other.zRot*(1-weight)+(float)p.otherZ()*sign;
        model.head.xRot+=(float)p.head();
        if(f.clip.inspection())model.head.yRot+=(float)(p.mainY()*.3)*sign;
        if(f.clip==ContextClip.BOW) {
            // Vanilla torso pivots at the shoulders. Rotate the entire upper body
            // around the hips instead, keeping its lower edge attached to the legs.
            float hipY=(model.rightLeg.y+model.leftLeg.y)*.5f;
            float hipZ=(model.rightLeg.z+model.leftLeg.z)*.5f;
            for(var part:new net.minecraft.client.model.geom.ModelPart[]{model.body,model.head,model.rightArm,model.leftArm}) {
                var offset=dev.shtark.rpganimation.animation.HipBend.rotate(part.y-hipY,part.z-hipZ,p.lean());
                part.y=hipY+(float)offset.y();part.z=hipZ+(float)offset.z();
                part.xRot+=(float)p.lean();
            }
        } else model.body.xRot+=(float)p.lean();
        model.body.zRot+=(float)p.roll()*sign;
        if(f.clip.seated()) {
            model.rightLeg.xRot+=(float)f.seatLeg;model.leftLeg.xRot+=(float)f.seatLeg;
            float spread=(float)(1-Math.abs(f.seatLeg)/1.35);
            model.rightLeg.yRot*=spread;model.leftLeg.yRot*=spread;
        }
        if(f.clip==ContextClip.STRETCH_MEDIUM||f.clip==ContextClip.STRETCH_LARGE) {
            model.rightLeg.xRot+=(float)(p.lean()*-.35);model.leftLeg.xRot+=(float)(p.lean()*-.35);
            model.rightLeg.zRot+=(float)(p.roll()*.3);model.leftLeg.zRot+=(float)(p.roll()*.3);
        }
    }
    private static void inspectionAnchor(PoseStack stack,ContextClip.Pose p,double side,float equip) {
        double w=p.weight();
        stack.translate(side*.56*(1-w),-.52+.30*w-.6*equip*(1-w),-.72-.18*w);
        stack.rotateDegrees(Axis.XP,(float)(p.mainX()*12));
        stack.rotateDegrees(Axis.YP,(float)(p.mainY()*20*side));
        stack.rotateDegrees(Axis.ZP,(float)(p.mainZ()*24*side));
    }
    public static void firstPerson(AvatarRenderState state,InteractionHand hand,PoseStack stack) {
        firstPerson(state,hand,stack,0);
    }
    public static void firstPerson(AvatarRenderState state,InteractionHand hand,PoseStack stack,float equip) {
        if(state==null)return;var f=state.getRenderData(FRAME);if(f==null)return;
        var p=f.pose;boolean main=(hand==InteractionHand.MAIN_HAND)==(right(f,state)==(state.mainArm==HumanoidArm.RIGHT));
        // The bobber renderer anchors its line to vanilla's rod transform. Keep that
        // hand aligned while the line exists; the assisting hand remains animated.
        if(f.clip.fishing()&&main&&Minecraft.getInstance().player!=null&&Minecraft.getInstance().player.fishing!=null)return;
        double x=main?p.mainX():p.otherX(),y=main?p.mainY():p.otherY(),z=main?p.mainZ():p.otherZ();
        double side=(hand==InteractionHand.MAIN_HAND?state.mainArm:state.mainArm.getOpposite())==HumanoidArm.RIGHT?1:-1;
        if(f.clip.inspection()) {
            // The item and both palms share one moving presentation frame. Account
            // for vanilla equip lowering here instead of letting it slide the item alone.
            inspectionAnchor(stack,p,side,equip);
            boolean flatItem=Minecraft.getInstance().player!=null
                    &&!(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem);
            if(flatItem)stack.rotateDegrees(Axis.YP,(float)(90*p.weight()*side));
            stack.translate(-side*.56,.52+.6*equip,.72);
            return;
        }
        stack.translate(-side*.08*Math.abs(y),-.10*p.lean()-.12*Math.sin(x),-.16*Math.sin(-x));
        stack.rotateDegrees(Axis.XP,(float)(x*22));stack.rotateDegrees(Axis.YP,(float)(y*30*side));
        stack.rotateDegrees(Axis.ZP,(float)(z*35*side));
    }
    @SubscribeEvent public static void extraHand(RenderHandEvent event) {
        if (!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed()) return;
        // Vanilla omits the empty offhand; submit it once when a two-hand clip needs it.
        if(event.getHand()!=InteractionHand.MAIN_HAND)return;
        var state=event.getPlayerState().avatarRenderState;
        if(state!=null&&state.getRenderData(BasicAnimations.FRAME)!=null&&state.getRenderData(BasicAnimations.FRAME).pose()!=null)return;
        if(state==null||state.isInvisible)return;var f=state.getRenderData(FRAME);if(f==null)return;
        var mc=Minecraft.getInstance();if(mc.player==null||!mc.player.getOffhandItem().isEmpty())return;
        var p=f.pose;if(p.weight()<=.001)return;
        if(!f.clip.inspection()&&!f.clip.usesSupportHand())return;
        if(f.clip.inspection()&&mc.player.getMainHandItem().isEmpty())return;
        var renderer=(net.minecraft.client.renderer.entity.player.AvatarRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(state);
        if(f.clip.inspection()) {
            // Held-item rendering supplies no skin arm. Submit the holding palm,
            // and a second palm only for two-handed inspection styles.
            var stack=event.getPoseStack();
            for(boolean right:new boolean[]{true,false}) {
                if(!f.clip.twoHandInspection()&&right!=(state.mainArm==HumanoidArm.RIGHT))continue;
                double side=right?1:-1;
                stack.pushPose();
                try {
                    inspectionAnchor(stack,p,state.mainArm==HumanoidArm.RIGHT?1:-1,event.getEquipProgress());
                    // Tuck the forearms downward while retaining the approved palm contact.
                    // Offsets compensate for the shorter forward projection at 50 degrees.
                    stack.translate(side*.26,-.39,.38);
                    stack.rotateDegrees(Axis.XP,-50);
                    stack.rotateDegrees(Axis.ZP,(float)(side*8));
                    stack.scale(.8f,.8f,.8f);
                    stack.translate(side*.3125,-.125,0);
                    if(right)renderer.renderRightHand(stack,event.getSubmitNodeCollector(),event.getPackedLight(),state.skin.body().texturePath(),state.showRightSleeve);
                    else renderer.renderLeftHand(stack,event.getSubmitNodeCollector(),event.getPackedLight(),state.skin.body().texturePath(),state.showLeftSleeve);
                } finally {stack.popPose();}
            }
            return;
        }
        boolean right=state.mainArm==HumanoidArm.LEFT;float side=right?1:-1;var stack=event.getPoseStack();stack.pushPose();
        try {
            stack.translate(0,-.8*(1-MotionCurve.ease(Math.min(1,p.weight()*3))),0);
            firstPerson(state,InteractionHand.OFF_HAND,stack);
            stack.translate(side*.48,-1.25,-.65);stack.rotateDegrees(Axis.XP,-25);stack.translate(side*.3125,-.125,0);
            if(right)renderer.renderRightHand(stack,event.getSubmitNodeCollector(),event.getPackedLight(),state.skin.body().texturePath(),state.showRightSleeve);
            else renderer.renderLeftHand(stack,event.getSubmitNodeCollector(),event.getPackedLight(),state.skin.body().texturePath(),state.showLeftSleeve);
        } finally {stack.popPose();}
    }
    public static void register(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier(){
            @Override public <T extends Avatar & ClientAvatarEntity> void accept(T avatar,AvatarRenderState state) {
                var f=frame(avatar.getUUID(),state.partialTick);
                if(state.hasPose(Pose.SLEEPING)||state.deathTime>0||BedAnimations.isRunning(avatar.getUUID())
                        ||ObserverAnimations.time(avatar.getUUID(),state.partialTick)!=null||DropAnimations.time(avatar.getUUID(),state.partialTick)!=null)f=null;
                if(f!=null&&f.clip==ContextClip.WALL&&avatar instanceof net.minecraft.world.entity.LivingEntity entity) {
                    double side=f.variant%2==0?1:-1,yaw=Math.toRadians(entity.getYRot());
                    var from=entity.position().add(0,1.15*state.scale,0);
                    var hit=entity.level().clip(new net.minecraft.world.level.ClipContext(from,
                            from.add(-Math.cos(yaw)*side*.7,0,-Math.sin(yaw)*side*.7),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,entity));
                    if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)f=null;
                    else {
                        double roll=ContextClip.wallRoll(hit.getLocation().distanceTo(from),state.scale);
                        var p=f.pose;
                        f=new Frame(f.clip,f.variant,new ContextClip.Pose(0,0,roll*p.weight(),p.otherX(),p.otherY(),p.otherZ(),
                                p.head(),0,0,p.weight()),f.seatLeg);
                    }
                }
                state.setRenderData(FRAME,f);
                var run=ACTIVE.get(avatar.getUUID());var offset=net.minecraft.world.phys.Vec3.ZERO;
                if(f!=null&&run!=null&&run.clip.seated()){
                    var delta=run.origin.subtract(new net.minecraft.world.phys.Vec3(state.x,state.y,state.z));
                    if(delta.lengthSqr()<=9)offset=delta.scale(DetailActivity.seatTravel(run.clock.time(ticks,state.partialTick),run.clip.duration));
                }
                var transfer=TRANSITIONS.get(avatar.getUUID());
                if(f!=null&&transfer!=null&&ticks+state.partialTick-transfer.start<transitionTicks(transfer)) {
                    double blend=MotionCurve.ease((ticks+state.partialTick-transfer.start)/transitionTicks(transfer));
                    offset=transfer.offset.scale(1-blend).add(offset.scale(blend));
                }
                state.setRenderData(OFFSET,offset);
                if(offset.lengthSqr()>1e-10)SEAT_OFFSETS.put(avatar.getUUID(),offset);else SEAT_OFFSETS.remove(avatar.getUUID());
            }
        });
    }
    @SubscribeEvent public static void pockets(PlayerInteractEvent.RightClickEmpty e) {
        if(e.getEntity()!=Minecraft.getInstance().player||e.getHand()!=InteractionHand.MAIN_HAND
                ||ticks<requestAfter||!ClientConfig.CONTEXT.get(ContextClip.POCKETS).get()||ClientConfig.REDUCED_MOTION.get())return;
        requestAfter=ticks+20;ClientPacketDistributor.sendToServer(new PocketRequest());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){checkDimension();if(!Minecraft.getInstance().isPaused())ticks++;
        TRANSITIONS.values().removeIf(t->ticks-t.start>=transitionTicks(t));
        ACTIVE.values().removeIf(r->r.clock.time(ticks,0)>=r.clip.duration);
        SEAT_OFFSETS.keySet().removeIf(id->!ACTIVE.containsKey(id)&&!TRANSITIONS.containsKey(id));}
    @SubscribeEvent public static void clone(ClientPlayerNetworkEvent.Clone e){ACTIVE.remove(e.getOldPlayer().getUUID());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){ACTIVE.clear();TRANSITIONS.clear();SEAT_OFFSETS.clear();dimension=null;ticks=0;requestAfter=0;}
}
