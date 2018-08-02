package grondag.acuity;

import org.lwjgl.input.Keyboard;

import grondag.acuity.api.AcuityRuntime;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.core.LightingModel;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
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
        {
            LightingModel oldModel = Configurator.lightingModel;
            ConfigManager.sync(Acuity.MODID, Config.Type.INSTANCE);
            if(Acuity.didEnabledStatusChange())
            {
                // important to reload renderers immediately in case 
                // this results in change of vbo to/from  displaylists
                Minecraft.getMinecraft().renderGlobal.loadRenderers();
                
                final boolean isEnabled = Acuity.isModEnabled();
                AcuityRuntime.INSTANCE.forEachListener(c -> c.onAcuityStatusChange(isEnabled));
                
                // Don't think this is needed because different interface for pipelined models
                // Minecraft.getMinecraft().refreshResources();
            }
            else if (oldModel != Configurator.lightingModel)
                AcuityRuntime.INSTANCE.forceReload();
        }
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
}
