package grondag.render_hooks;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import grondag.render_hooks.api.IRenderHookRuntime;
import grondag.render_hooks.api.RenderHookRuntime;
import grondag.render_hooks.core.OpenGlHelperExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(   modid = RenderHooks.MODID, 
        name = RenderHooks.MODNAME,
        version = RenderHooks.VERSION,
        acceptedMinecraftVersions = "[1.12]", 
        clientSideOnly = true)

public class RenderHooks
{
	public static final String MODID = "render_hooks";
	public static final String MODNAME = "Render Hooks";
	public static final String VERSION = "0.0.1";
	
	@Instance
	public static RenderHooks INSTANCE = new RenderHooks();
	
	@SideOnly(Side.CLIENT)
	public static final boolean isModEnabled()
	{
	    return ASMTransformer.allPatchesSuccessful() && Configurator.enabled;
	}
	
	@SideOnly(Side.CLIENT)
	private static boolean lastEnabledSetting = isModEnabled();
	
	/**
	 * For use by config event handler.
	 */
	@SideOnly(Side.CLIENT)
	public static boolean didEnabledStatusChange()
	{
	    final boolean current = isModEnabled();
	    if(current != lastEnabledSetting)
	    {
	        lastEnabledSetting = current;
	        return true;
	    }
	    else 
	        return false;
	}
	
    @Nullable
    private static Logger log;
    
    public Logger getLog()
    {
        Logger result = log;
        if(result == null)
        {
            result = LogManager.getLogger(MODNAME);
            log = result;
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    @EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
	}

    @SideOnly(Side.CLIENT)
	@EventHandler
	public void init(FMLInitializationEvent event)
	{

	}

    @SideOnly(Side.CLIENT)
	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
        // try to get faster access to GL calls
        OpenGlHelperExt.initialize();
        
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if(rm instanceof IReloadableResourceManager)
        {
            ((IReloadableResourceManager)rm).registerReloadListener(new IResourceManagerReloadListener() {

                @Override
                public void onResourceManagerReload(IResourceManager resourceManager)
                {
                    RenderHookRuntime.INSTANCE.forceReload();
                }});
        }
        
	}
	
    @SideOnly(Side.CLIENT)
	@Mod.EventHandler
    public void imcCallback(FMLInterModComms.IMCEvent event)
	{
        for (FMLInterModComms.IMCMessage message : event.getMessages())
        {
            if (message.key.equalsIgnoreCase("getRenderHookRuntime"))
            {
                Optional<Function<IRenderHookRuntime, Void>> value = message.getFunctionValue(IRenderHookRuntime.class, Void.class);
                if (value.isPresent()) 
                    value.get().apply(RenderHookRuntime.INSTANCE);
                else 
                    getLog().warn("Error in inter-mod communication request for RenderHooks runtime.");
            }
        }
    }
}