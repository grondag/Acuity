package grondag.render_hooks;

import grondag.render_hooks.api.ProgramManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.PostConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
            ConfigManager.sync(RenderHooks.MODID, Config.Type.INSTANCE);
            if(RenderHooks.didEnabledStatusChange())
                Minecraft.getMinecraft().refreshResources();
        }
    }
    
    @SubscribeEvent()
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if(event.phase == Phase.START) 
            ProgramManager.INSTANCE.onRenderTick();
    }
    
    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) 
    {
        if(event.phase == Phase.START) 
            ProgramManager.INSTANCE.onGameTick();
    }
}
