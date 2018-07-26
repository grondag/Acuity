package grondag.render_hooks;

import org.lwjgl.input.Keyboard;

import grondag.render_hooks.api.ProgramManager;
import grondag.render_hooks.api.RenderHookRuntime;
import grondag.render_hooks.core.LightingModel;
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
        if(event.getModID().equals(RenderHooks.MODID))
        {
            LightingModel oldModel = Configurator.lightingModel;
            ConfigManager.sync(RenderHooks.MODID, Config.Type.INSTANCE);
            if(RenderHooks.didEnabledStatusChange())
                Minecraft.getMinecraft().refreshResources();
            else if (oldModel != Configurator.lightingModel)
                RenderHookRuntime.INSTANCE.forceReload();
        }
    }
    
    @SubscribeEvent()
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if(event.phase == Phase.START) 
            ProgramManager.INSTANCE.onRenderTick(event);
    }
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) 
    {
        if(event.phase == Phase.START) 
            ProgramManager.INSTANCE.onGameTick(event);
    }
    
    @SubscribeEvent
    public static void onKeyInput(KeyInputEvent event) 
    {
        if(Keyboard.isKeyDown(61) && Keyboard.getEventKey() == 30)
            RenderHookRuntime.INSTANCE.forceReload();
    }
}
