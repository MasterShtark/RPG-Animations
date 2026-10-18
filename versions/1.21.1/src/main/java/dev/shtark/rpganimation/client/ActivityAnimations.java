package dev.shtark.rpganimation.client;

import dev.shtark.rpganimation.*;
import dev.shtark.rpganimation.animation.ActivityState;
import dev.shtark.rpganimation.mixin.ChatInputAccessor;
import java.util.*;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.HumanoidModel;
import dev.shtark.rpganimation.client.pose.PlayerFrame;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import dev.shtark.rpganimation.client.pose.PoseKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import com.mojang.math.Axis;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.shtark.rpganimation.client.pose.*;

@EventBusSubscriber(modid=RpgAnimation.ID,value=Dist.CLIENT)
public final class ActivityAnimations {
    private static final class Status {
        int kind,intensity,previous;long changed,received,phase;
        ActivityState.Pose origin=new ActivityState.Pose(0,0,0,0);
        double originWeight;
        Status(int kind,int intensity,long phase){this.kind=kind;this.intensity=intensity;this.phase=phase;changed=received=ticks;}
    }
    public record Frame(UUID player,int kind,int intensity,double age,ActivityState.Pose pose,double weight) {}
    private static final PoseKey<Frame> FRAME=new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"activity_pose"));
    private static final PoseKey<Component> LABEL=new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"activity_label"));
    private static final PoseKey<UUID> PANEL=new PoseKey<>(ResourceLocation.fromNamespaceAndPath(RpgAnimation.ID,"activity_panel"));
    private static final Map<UUID,Status> STATES=new HashMap<>();
    private static SubscriptionRequest subscription;
    private static long nextSubscription,lastSubscriptionSend;
    private static int subscriptionGeneration;
    private static long ticks,lastInput,lastTyped,lastClick,lastSend=-100;
    private static int sentKind=-1,sentIntensity=-1;
    private static Object level,screen;
    private static double x,y,z,mouseX,mouseY;
    private static float yaw,pitch;

    public static void accept(ActivityPayload p) {
        if(!ActivityState.valid(p.kind(),p.intensity())||p.phase()<0)return;
        var s=STATES.get(p.player());
        if(s==null){if(p.kind()!=0)STATES.put(p.player(),new Status(p.kind(),p.intensity(),p.phase()));return;}
        if(s.kind!=p.kind()||s.intensity!=p.intensity()){
            var current=frame(s,0);s.origin=current.pose;s.originWeight=current.weight;
            s.previous=s.kind;s.kind=p.kind();s.changed=ticks;
        }
        s.intensity=p.intensity();s.received=ticks;s.phase=p.phase();
    }
    private static void input(){lastInput=ticks;}
    @SubscribeEvent public static void key(InputEvent.Key e){if(e.getAction()!=0)input();}
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post e){input();lastClick=ticks;}
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent e){input();}
    @SubscribeEvent public static void screenKey(ScreenEvent.KeyPressed.Pre e){input();}
    @SubscribeEvent public static void character(ScreenEvent.CharacterTyped.Pre e){
        input();
        // Post is skipped when the chat edit box consumes the character.
        if(e.getScreen() instanceof ChatScreen){lastTyped=ticks;}
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Post e){input();lastClick=ticks;}
    @SubscribeEvent public static void screenScroll(ScreenEvent.MouseScrolled.Post e){input();}
    @SubscribeEvent public static void drag(ScreenEvent.MouseDragged.Post e){input();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();var p=mc.player;
        if(mc.level!=level){reset();level=mc.level;}
        if(p==null||mc.level==null)return;
        if(mc.isPaused())return;
        ticks++;
        var desired=new SubscriptionRequest(subscriptionGeneration,ClientConfig.VIEW_ACTIVITY.get(),ClientConfig.VIEW_ACTIVITY.get()&&ClientConfig.ACTIVITY_TAB.get(),
                ClientConfig.VIEW_ACTIVITY.get()&&ClientConfig.ACTIVITY_PANEL.get()&&ClientConfig.VIEW_INVENTORY_PREVIEW.get(),ClientConfig.SHARE_ACTIVITY.get()&&ClientConfig.SHARE_BROWSING.get()&&ClientConfig.SHARE_INVENTORY_PREVIEW.get(),ClientConfig.ACTIVITY_DISTANCE.get());
        if(ticks>=nextSubscription&&(!desired.equals(subscription)||ticks-lastSubscriptionSend>=100)) {
            PacketDistributor.sendToServer(desired);subscription=desired;nextSubscription=ticks+20;lastSubscriptionSend=ticks;
        }
        Object nowScreen=mc.screen;
        double mx=mc.mouseHandler.xpos(),my=mc.mouseHandler.ypos();
        if(nowScreen!=screen||p.getX()!=x||p.getY()!=y||p.getZ()!=z||p.getYRot()!=yaw||p.getXRot()!=pitch
                ||mx!=mouseX||my!=mouseY||p.isUsingItem()||p.swinging||p.fishing!=null)input();
        screen=nowScreen;x=p.getX();y=p.getY();z=p.getZ();yaw=p.getYRot();pitch=p.getXRot();mouseX=mx;mouseY=my;
        int kind=0,intensity=0;
        if(ClientConfig.SHARE_ACTIVITY.get()&&p.isAlive()&&!p.isSpectator()&&!p.isSleeping()) {
            if(ClientConfig.SHARE_IDLE.get()&&!(nowScreen instanceof ChatScreen)&&PreviewScreens.bounds(mc.screen)==null
                    &&ticks-lastInput>=ClientConfig.IDLE_SECONDS.get()*20L)kind=ActivityState.IDLE;
            else if(nowScreen instanceof ChatScreen chat&&ClientConfig.SHARE_TYPING.get()) {
                var box=((ChatInputAccessor)chat).rpgAnimation$input();
                // Inspect only the command prefix locally; never retain or send its contents.
                if(box!=null&&(!box.getValue().stripLeading().startsWith("/")||ClientConfig.SHARE_COMMANDS.get())) {
                    kind=ActivityState.TYPING;intensity=ActivityState.typingIntensity(ticks-lastTyped);
                }
            } else if(PreviewScreens.bounds(mc.screen)!=null &&ClientConfig.SHARE_BROWSING.get()) {
                kind=ActivityState.BROWSING;intensity=ticks-lastClick<8?3:0;
            }
        }
        if(ticks-lastSend>=ActivityState.MIN_INTERVAL&&
                (kind!=sentKind||intensity!=sentIntensity||(kind!=0&&ticks-lastSend>=ActivityState.HEARTBEAT))) {
            PacketDistributor.sendToServer(new ActivityRequest(kind,intensity));
            sentKind=kind;sentIntensity=intensity;lastSend=ticks;
        }
        STATES.values().removeIf(s->(s.kind==0&&ticks-s.changed>=20));
    }
    private static boolean gesture(int kind) {
        return !ClientConfig.REDUCED_MOTION.get()&&switch(kind){
            case 1->ClientConfig.TYPING_GESTURE.get();case 2->ClientConfig.BROWSING_GESTURE.get();case 3->ClientConfig.IDLE_GESTURE.get();default->false;};
    }
    private static Frame frame(Status s,double partial) {
        double age=ticks-s.changed+partial;
        boolean typing=s.kind==ActivityState.TYPING||s.previous==ActivityState.TYPING;
        double w=ActivityState.blend(typing?age*.5:age),target=s.kind==0?0:1;
        var p=ActivityState.pose(s.kind,s.intensity,s.phase+ticks-s.received+partial);var o=s.origin;
        var pose=new ActivityState.Pose(o.right()*(1-w)+p.right()*w*target,o.left()*(1-w)+p.left()*w*target,
                o.head()*(1-w)+p.head()*w*target,o.roll()*(1-w)+p.roll()*w*target);
        return new Frame(null,s.kind==0?s.previous:s.kind,s.intensity,age,pose,s.originWeight*(1-w)+target*w);
    }
    private static Component label(int kind) {
        return switch(kind){case 1->Component.literal("[ "+".".repeat(1+(int)(ticks/8%3))+" ]");
            case 2->Component.translatable("rpg_animation.activity.browsing");case 3->Component.literal("Zzz");default->null;};
    }
    public static Component tab(UUID id) {
        if(!ClientConfig.VIEW_ACTIVITY.get()||!ClientConfig.ACTIVITY_TAB.get())return null;
        var s=STATES.get(id);if(s==null||s.kind==0)return null;
        String key=switch(s.kind){case 1->"typing";case 2->"browsing";default->"idle";};
        return Component.literal(" [").append(Component.translatable("rpg_animation.activity."+key)).append("]")
                .withStyle(net.minecraft.ChatFormatting.GRAY);
    }
    public static void register(PoseRegistry event) {
        event.registerAvatarEntityModifier(new PoseSampler(){
            @Override public void accept(net.minecraft.world.entity.LivingEntity avatar,PlayerFrame state) {
                Frame frame=null;var s=STATES.get(avatar.getUUID());
                if(s!=null&&s.kind==ActivityState.IDLE&&avatar instanceof net.minecraft.world.entity.player.Player player&&player.fishing!=null)s=null;
                if(s!=null&&ClientConfig.VIEW_ACTIVITY.get()&&state.distanceToCameraSq<=Math.pow(ClientConfig.ACTIVITY_DISTANCE.get(),2)&&!state.isInvisible&&!state.hasPose(Pose.SLEEPING)&&state.deathTime<=0
                        &&avatar instanceof net.minecraft.world.entity.LivingEntity entity&&!entity.isUsingItem()&&entity.hurtTime==0
                        &&!entity.isPassenger()&&!entity.isFallFlying()&&!entity.isSwimming()
                        &&!BedAnimations.isRunning(avatar.getUUID())&&ObserverAnimations.time(avatar.getUUID(),0)==null
                        &&DropAnimations.time(avatar.getUUID(),0)==null&&PickupAnimations.time(avatar.getUUID(),0)==null
                        ) {
                    var sampled=frame(s,state.partialTick);
                    frame=new Frame(avatar.getUUID(),sampled.kind,sampled.intensity,sampled.age,sampled.pose,sampled.weight);
                }
                state.setRenderData(FRAME,frame);
                state.setRenderData(PANEL,s!=null&&s.kind==ActivityState.BROWSING&&ClientConfig.VIEW_ACTIVITY.get()
                        &&ClientConfig.ACTIVITY_PANEL.get()&&ClientConfig.VIEW_INVENTORY_PREVIEW.get()
                        &&!state.isInvisible&&state.deathTime<=0
                        &&state.distanceToCameraSq<=Math.pow(ClientConfig.ACTIVITY_DISTANCE.get(),2)?avatar.getUUID():null);
                state.setRenderData(LABEL,null);
                var context=state.getRenderData(ContextAnimations.FRAME);
                boolean socialGesture=context!=null&&context.clip().social();
                if(s!=null&&ClientConfig.VIEW_ACTIVITY.get()&&!state.isInvisible&&state.nameTag!=null
                        &&!(s.kind==ActivityState.IDLE&&socialGesture)
                        &&state.distanceToCameraSq<=Math.pow(ClientConfig.ACTIVITY_DISTANCE.get(),2)) {
                    boolean show=s.kind==1?ClientConfig.BUBBLES.get():s.kind==2?ClientConfig.BROWSING_INDICATOR.get():ClientConfig.IDLE_INDICATOR.get();
                    if(show)state.setRenderData(LABEL,label(s.kind));
                }
            }
        });
    }
    public static String actionKey(PlayerFrame state){var f=state.getRenderData(FRAME);
        return f==null?null:f.kind==1?"activity_typing":f.kind==2?"activity_browsing":"activity_idle";}
    public static boolean typingHands() {
        var p=Minecraft.getInstance().player;if(p==null||!gesture(ActivityState.TYPING))return false;
        var s=STATES.get(p.getUUID());if(s==null)return false;
        var f=frame(s,0);
        return f.kind==ActivityState.TYPING&&f.weight>.001&&p.isAlive()&&!p.isSpectator()&&!p.isInvisible()
                &&!p.isSleeping()&&!p.isUsingItem()&&!p.isPassenger()&&!p.isFallFlying()&&!p.isSwimming()&&p.hurtTime==0
                &&!IntroController.active()&&!BedAnimations.isRunning(p.getUUID())&&!DropAnimations.localActive()
                &&!PickupAnimations.localActive()&&!ContextAnimations.localActive();
    }
    /** Lower vanilla hands before replacing their render path, then restore them after the custom arms leave. */
    public static double typingHandoff(float partial) {
        if(!typingHands()||!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("activity_typing"))return 0;
        var mc=Minecraft.getInstance();
        return dev.shtark.rpganimation.animation.ActivityState.handHandoff(frame(STATES.get(mc.player.getUUID()),partial).weight);
    }
    public static boolean submitTypingHands(float partial,com.mojang.blaze3d.vertex.PoseStack stack,
            net.minecraft.client.renderer.MultiBufferSource collector,dev.shtark.rpganimation.client.pose.PlayerFrame player) {
        if(!typingHands()||!dev.shtark.rpganimation.client.compat.AnimationCompatibility.localPresentationAllowed("activity_typing"))return false;
        var avatar=player;if(avatar==null)return false;
        var mc=Minecraft.getInstance();var f=frame(STATES.get(mc.player.getUUID()),partial);
        if(f.weight<.2)return false;
        net.minecraft.client.renderer.entity.player.PlayerRenderer renderer=(net.minecraft.client.renderer.entity.player.PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(avatar.player);
        for(boolean right:new boolean[]{true,false}) {
            double angle=right?f.pose.right():f.pose.left(),side=right?1:-1;
            stack.pushPose();
            try {
                double lift=dev.shtark.rpganimation.animation.ActivityState.typingLift(f.weight);
                stack.translate(side*.22,-1.0-1.8*(1-lift),-.6);
                stack.mulPose(Axis.XP.rotationDegrees((float)(angle*180/Math.PI)));
                stack.mulPose(Axis.ZP.rotationDegrees((float)(side*8*f.weight)));
                stack.translate(side*.3125,-.125,0);
                if(right)renderer.renderRightHand(stack,collector,avatar.lightCoords,avatar.player);
                else renderer.renderLeftHand(stack,collector,avatar.lightCoords,avatar.player);
            } finally {stack.popPose();}
        }
        return true;
    }
    public static void model(HumanoidModel<?> model,PlayerFrame state) {
        var f=state.getRenderData(FRAME);if(f==null||!gesture(f.kind))return;
        var context=state.getRenderData(ContextAnimations.FRAME);
        float remaining=context!=null&&context.clip().inspection()?(float)(1-context.pose().weight()):1;
        float w=(float)f.weight*remaining;var p=f.pose;
        model.rightArm.xRot=model.rightArm.xRot*(1-w)+(float)p.right()*remaining;
        model.leftArm.xRot=model.leftArm.xRot*(1-w)+(float)p.left()*remaining;
        model.head.xRot=model.head.xRot*(1-w)+(float)p.head()*remaining;model.body.zRot+=(float)p.roll()*remaining;
    }
    public static void draw(PlayerFrame state,com.mojang.blaze3d.vertex.PoseStack stack,
                            net.minecraft.client.renderer.MultiBufferSource collector) {
        if(state.isInvisible||dev.shtark.rpganimation.client.compat.AnimationCompatibility.externalPoseOwned(state))return;
        var text=state.getRenderData(LABEL);
        if(text!=null&&state.nameTag!=null) {
            var mc=Minecraft.getInstance();stack.pushPose();
            try {
                stack.translate(0,state.player.getBbHeight()+.8,0);
                stack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());stack.scale(.025f,-.025f,.025f);
                mc.font.drawInBatch(text,-mc.font.width(text)/2f,0,0xffffffff,true,stack.last().pose(),collector,
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,state.lightCoords);
            } finally {stack.popPose();}
        }
        var avatar=state;
        var panel=avatar.getRenderData(PANEL);
        if(panel==null||!ClientConfig.ACTIVITY_PANEL.get()||avatar.distanceToCameraSq>Math.pow(ClientConfig.ACTIVITY_DISTANCE.get(),2))return;
        stack.pushPose();
        try {
            stack.translate(0,1.05,0);stack.mulPose(Axis.YP.rotationDegrees(-avatar.bodyRot));stack.translate(0,0,.48);
            stack.mulPose(Axis.XP.rotationDegrees(35));
            InventoryScreenPreview.draw(panel,stack,collector,state.lightCoords);

        } finally {stack.popPose();}
    }
    private static void reset(){subscriptionGeneration++;subscription=null;nextSubscription=lastSubscriptionSend=0;STATES.clear();ticks=lastInput=lastTyped=lastClick=0;lastSend=-100;sentKind=sentIntensity=-1;screen=null;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){reset();level=null;}
}



