package grondag.acuity;

import org.lwjgl.input.Keyboard;

import grondag.acuity.api.AcuityRuntime;
import grondag.acuity.api.PipelineManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.PostConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class EventHandler
{
    @SubscribeEvent
    public static void onPostConfigChanged(PostConfigChangedEvent event) 
    {
        if(event.getModID().equals(Acuity.MODID))
            Configurator.handleChange(event);
    }
    
    @SubscribeEvent()
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if(event.phase == Phase.START) 
            PipelineManager.INSTANCE.onRenderTick(event);
    }
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) 
    {
        if(event.phase == Phase.START) 
            PipelineManager.INSTANCE.onGameTick(event);
    }
    
    @SubscribeEvent
    public static void onKeyInput(KeyInputEvent event) 
    {
        if(Keyboard.isKeyDown(61) && Keyboard.getEventKey() == 30)
            AcuityRuntime.INSTANCE.forceReload();
    }
    
//    final static Frustum camera = new Frustum();
//    
//    @SubscribeEvent
//    public static void onRenderLast(RenderWorldLastEvent event) 
//    {
//        Tessellator tessellator = Tessellator.getInstance();
//        
//        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
//        if(entity == null) return;
//        float partialTicks = Animation.getPartialTickTime();
//        
//        double cameraX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
//        double cameraY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
//        double cameraZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
//        camera.setPosition(cameraX, cameraY, cameraZ);
//        
//        BufferBuilder bufferBuilder = tessellator.getBuffer();
//        bufferBuilder.setTranslation(-cameraX, -cameraY, -cameraZ);
//        
//        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
//        GlStateManager.enableBlend();
//        GlStateManager.disableTexture2D();
//        GlStateManager.depthMask(false);
//        // prevent z-fighting
//        GlStateManager.enablePolygonOffset();
//        GlStateManager.doPolygonOffset(-1, -1);
//        GlStateManager.enableDepth();
//        
//        long now = System.nanoTime();
//        
//        CompoundBufferBuilder.SORTS.forEach((BlockPos pos, Long age) ->
//        {
//            float opacity = (float) (Math.max(3000000000L - (now - age), 0) / 3000000000.0);
//            if(opacity <= 0)
//                return;
//            
//            AxisAlignedBB box = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), 
//                    pos.getX() + 16, pos.getY() + 16, pos.getZ() + 16);
//            if(!camera.isBoundingBoxInFrustum(box)) 
//                return;
//            
//            bufferBuilder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
//            RenderGlobal.addChainedFilledBoxVertices(bufferBuilder, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 
//                    opacity, 1f, 1- opacity, opacity * 0.4f);
//            tessellator.draw();
//        });
//        
//        bufferBuilder.setTranslation(0, 0, 0);
//        
//        GlStateManager.disablePolygonOffset();
//        
//        GlStateManager.depthMask(true);
//        GlStateManager.enableTexture2D();
//        GlStateManager.disableBlend();
//        GlStateManager.enableAlpha();
//    }
}
