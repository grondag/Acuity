package grondag.render_hooks;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import grondag.render_hooks.api.IRenderHookRuntime;
import grondag.render_hooks.api.impl.RenderHookRuntime;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

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
	
	public final RenderHookRuntime runtime = new RenderHookRuntime();
	
	public static final boolean isModEnabled()
	{
	    return ASMTransformer.allPatchesSuccessful() && Configurator.enabled;
	}
	
	private static boolean lastEnabledSetting = isModEnabled();
	/**
	 * For use by config event handler.
	 */
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

    @EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{

	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{

	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{

	}
	
	@Mod.EventHandler
    public void imcCallback(FMLInterModComms.IMCEvent event)
	{
        for (FMLInterModComms.IMCMessage message : event.getMessages())
        {
            if (message.key.equalsIgnoreCase("getRenderHookRuntime"))
            {
                Optional<Function<IRenderHookRuntime, Void>> value = message.getFunctionValue(IRenderHookRuntime.class, Void.class);
                if (value.isPresent()) 
                    value.get().apply(runtime);
                else 
                    log.warn("Error in inter-mod communication request for RenderHooks runtime.");
            }
        }
    }
}