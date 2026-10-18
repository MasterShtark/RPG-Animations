package dev.shtark.rpganimation.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.shtark.rpganimation.*;
import dev.shtark.rpganimation.animation.InventoryPreview;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Opt-in inventory crop, captured after GUI rendering. Full frames are never sent or saved. */
@EventBusSubscriber(modid=RpgAnimation.ID,value=Dist.CLIENT)
public final class InventoryScreenPreview {
    private record Crop(net.minecraft.client.gui.screens.Screen screen,int x,int y,int width,int height,int guiWidth,int guiHeight) {}
    private static final class Preview {
        final Identifier texture;final DynamicTexture dynamic;final int width,height;long expires;long born;
        Preview(Identifier texture,DynamicTexture dynamic,int width,int height,long expires){this.texture=texture;this.dynamic=dynamic;this.width=width;this.height=height;this.expires=expires;}
    }
    private static final Map<UUID,Preview> PREVIEWS=new HashMap<>();
    private static final dev.shtark.rpganimation.animation.PreviewCadence CAPTURE_RATE=new dev.shtark.rpganimation.animation.PreviewCadence();
    private static Crop crop;
    private static final boolean DIAGNOSTICS=Boolean.getBoolean("rpg_animation.previewDiagnostics");
    private static long sentImages,receivedImages,drawnPanels,diagnosticAt;
    private static String lastContainer="none";
    private static boolean observedContainerRender;
    private static long nextCapture,ticks;
    private static boolean pending,shared,serverAllowed;
    private static int maxWidth=96,interval=40,generation;
    private static final dev.shtark.rpganimation.animation.PreviewChanges changes=new dev.shtark.rpganimation.animation.PreviewChanges();
    public static void policy(PreviewPolicyPayload p) {
        var current=Minecraft.getInstance().level;if(level!=current){clear();level=current;}
        serverAllowed=p.allowed();maxWidth=Math.clamp(p.width(),32,128);interval=Math.clamp(p.interval(),20,100);
        changes.invalidate();generation++;
    }
    private static Object level;
    @SubscribeEvent public static void extracted(ScreenEvent.Render.Post event) {
        var mc=Minecraft.getInstance();
        var screen=event.getScreen();var bounds=PreviewScreens.bounds(screen);
        if(bounds!=null) {
            if(DIAGNOSTICS){lastContainer=screen.getClass().getName();observedContainerRender=true;}
            if(enabled()&&(crop==null||crop.screen!=screen)){nextCapture=0;changes.invalidate();}
            crop=enabled()?new Crop(screen,bounds.x(),bounds.y(),bounds.width(),bounds.height(),
                    mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight()):null;
        } else crop=null;
    }
    private static boolean enabled() {
        return serverAllowed&&ClientConfig.SHARE_ACTIVITY.get()&&ClientConfig.SHARE_BROWSING.get()&&ClientConfig.SHARE_INVENTORY_PREVIEW.get();
    }
    public static void capture(RenderTarget target) {
        var mc=Minecraft.getInstance();var c=crop;
        long now=System.nanoTime();
        if(pending||now<nextCapture||c==null||!enabled()||mc.player==null||mc.gui.screen()!=c.screen)return;
        if(c.width<=0||c.height<=0||c.guiWidth<=0||c.guiHeight<=0||target.width<=0||target.height<=0
                ||mc.getWindow().getGuiScaledWidth()!=c.guiWidth||mc.getWindow().getGuiScaledHeight()!=c.guiHeight)return;
        if(!CAPTURE_RATE.acquire(now/50_000_000L,interval)){nextCapture=now+50_000_000L;return;}
        pending=true;nextCapture=now+interval*50_000_000L;Object session=mc.level;int captureGeneration=generation;
        try { Screenshot.takeScreenshot(target,image->{
            try(image) {
                if(captureGeneration!=generation||mc.level!=session||mc.gui.screen()!=c.screen||!enabled()
                        ||mc.getWindow().getGuiScaledWidth()!=c.guiWidth||mc.getWindow().getGuiScaledHeight()!=c.guiHeight)return;
                double scale=Math.min((double)maxWidth/c.width,(double)InventoryPreview.MAX_HEIGHT/c.height);
                int width=Math.max(1,(int)(c.width*scale)),height=Math.max(1,(int)(c.height*scale));
                byte[] bytes=new byte[width*height*2];
                for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                    int sx=(int)((c.x+(x+.5)*c.width/width)*image.getWidth()/c.guiWidth);
                    int sy=(int)((c.y+(y+.5)*c.height/height)*image.getHeight()/c.guiHeight);
                    // Never expand the crop into the surrounding world or another screen.
                    if(sx<0||sy<0||sx>=image.getWidth()||sy>=image.getHeight())return;
                    int value=InventoryPreview.encode(Integer.reverseBytes(image.getPixel(sx,sy))>>>8),index=(x+y*width)*2;
                    bytes[index]=(byte)(value>>>8);bytes[index+1]=(byte)value;
                }
                mc.execute(()->{
                    if(captureGeneration==generation&&mc.level==session&&mc.gui.screen()==c.screen&&enabled()) {
                        if(DIAGNOSTICS)sentImages++;
                        boolean unchanged=!changes.update(width,height,bytes);
                        ClientPacketDistributor.sendToServer(unchanged?new InventoryPreviewRequest(changes.revision(),-1,0,new byte[0]):new InventoryPreviewRequest(changes.revision(),width,height,bytes));shared=true;
                    }
                });
            } finally {pending=false;}
        }); } catch(RuntimeException failure){pending=false;throw failure;}
    }
    public static void accept(InventoryPreviewPayload payload) {
        if(!InventoryPreview.valid(payload.width(),payload.height(),payload.pixels().length))return;
        var existing=PREVIEWS.get(payload.player());
        if(payload.width()==0||!ClientConfig.VIEW_ACTIVITY.get()||!ClientConfig.VIEW_INVENTORY_PREVIEW.get()||!ClientConfig.ACTIVITY_PANEL.get()){remove(payload.player());return;}
        if(payload.width()==-1){if(existing!=null)existing.expires=ticks+400;return;}
        if(DIAGNOSTICS)receivedImages++;
        boolean reuse=existing!=null&&existing.width==payload.width()&&existing.height==payload.height();
        long born=existing==null?ticks:existing.born;
        if(!reuse){remove(payload.player());if(PREVIEWS.size()>=32)return;}
        var pixels=reuse?existing.dynamic.getPixels():new NativeImage(payload.width(),payload.height(),false);
        for(int y=0;y<payload.height();y++)for(int x=0;x<payload.width();x++) {
            int i=(x+y*payload.width())*2;
            pixels.setPixelABGR(x,y,InventoryPreview.decode((payload.pixels()[i]&255)<<8|payload.pixels()[i+1]&255));
        }
        if(reuse){existing.dynamic.upload();existing.expires=ticks+400;return;}
        var id=Identifier.fromNamespaceAndPath(RpgAnimation.ID,"inventory_preview/"+payload.player());
        var texture=new DynamicTexture(()->"Shared inventory preview",pixels);
        Minecraft.getInstance().getTextureManager().register(id,texture);
        var created=new Preview(id,texture,payload.width(),payload.height(),ticks+400);created.born=born;
        PREVIEWS.put(payload.player(),created);
    }

    public static void draw(UUID player,PoseStack stack,SubmitNodeCollector collector,int light) {
        if(!ClientConfig.VIEW_INVENTORY_PREVIEW.get())return;
        var preview=PREVIEWS.get(player);if(preview==null)return;
        if(DIAGNOSTICS)drawnPanels++;
        double age=ticks+Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)-preview.born;
        int tint=0xFFFFFFFF;
        // Opaque depth-writing panel; ease its size instead of revealing rear translucent geometry.
        float w=(float)(.36*(.92+.08*dev.shtark.rpganimation.animation.MotionCurve.ease(age/4))),h=w*preview.height/preview.width;
        collector.submitCustomGeometry(stack,RenderTypes.entityCutout(preview.texture,false),(pose,buffer)->{
            // Both sides use the original screen orientation.
            for(int side=0;side<2;side++) {
                for(int i=0;i<4;i++) {
                    float x=(i<2?-w:w)*(side==0?1:-1),y=(i==0||i==3)?h:-h;
                    buffer.addVertex(pose,x,y,side==0?.001f:-.001f).setColor(tint).setUv(i<2?0:1,(i==0||i==3)?0:1)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose,0,0,side==0?1:-1);
                }
            }
        });
    }
    private static void remove(UUID id) {
        var old=PREVIEWS.remove(id);if(old!=null)Minecraft.getInstance().getTextureManager().release(old.texture);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();ticks++;
        if(level!=mc.level){clear();level=mc.level;}
        if(DIAGNOSTICS&&mc.level!=null&&ticks>=diagnosticAt) {
            diagnosticAt=ticks+100;
            var screen=mc.gui.screen();
            com.mojang.logging.LogUtils.getLogger().info(
                    "RPG preview diagnostic: screen={} lastContainer={} renderEvent={} policy={} consent={} captureEnabled={} crop={} pending={} sent={} received={} textures={} drawn={}",
                    screen==null?"none":screen.getClass().getName(),lastContainer,observedContainerRender,serverAllowed,
                    ClientConfig.SHARE_INVENTORY_PREVIEW.get(),enabled(),crop!=null,pending,sentImages,receivedImages,PREVIEWS.size(),drawnPanels);
            observedContainerRender=false;
        }
        // A screen can close before the first successful capture/lease. Do not retain
        // its menu/player graph until another screen happens to render.
        if(crop!=null&&mc.gui.screen()!=crop.screen){crop=null;generation++;}
        if(shared&&(PreviewScreens.bounds(mc.gui.screen())==null||!enabled())&&mc.player!=null) {
            ClientPacketDistributor.sendToServer(new InventoryPreviewRequest(changes.revision(),0,0,new byte[0]));shared=false;crop=null;changes.invalidate();generation++;
        }
        boolean hidden=!ClientConfig.VIEW_INVENTORY_PREVIEW.get()||!ClientConfig.VIEW_ACTIVITY.get()||!ClientConfig.ACTIVITY_PANEL.get();
        var iterator=PREVIEWS.values().iterator();
        while(iterator.hasNext()) {
            var preview=iterator.next();
            if(preview.expires<=ticks||hidden){mc.getTextureManager().release(preview.texture);iterator.remove();}
        }
    }
    private static void clear(){CAPTURE_RATE.reset();sentImages=receivedImages=drawnPanels=diagnosticAt=0;lastContainer="none";observedContainerRender=false;generation++;serverAllowed=false;changes.invalidate();for(var id:new ArrayList<>(PREVIEWS.keySet()))remove(id);crop=null;shared=false;nextCapture=0;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();level=null;}
}
