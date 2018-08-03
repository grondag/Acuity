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
}
