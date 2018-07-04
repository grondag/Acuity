package grondag.render_hooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
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

    @Nullable
    private static Logger log;
    
    public Logger getLog()
    {
        Logger result = log;
        // allow access to log during unit testing or other debug scenarios
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
}