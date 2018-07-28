package grondag.acuity;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import grondag.acuity.api.IAcuityRuntime;
import grondag.acuity.api.AcuityRuntime;
import grondag.acuity.core.OpenGlHelperExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SuppressWarnings("deprecation")
@Mod(   modid = Acuity.MODID, 
        name = Acuity.MODNAME,
        version = Acuity.VERSION,
        acceptedMinecraftVersions = "[1.12]", 
        clientSideOnly = true)

public class Acuity
{
	public static final String MODID = "acuity";
	public static final String MODNAME = "Acuity Rendering API";
	public static final String VERSION = "0.0.1";
	
	@Instance
	public static Acuity INSTANCE = new Acuity();
	
	@SideOnly(Side.CLIENT)
	public static final boolean isModEnabled()
	{
	    return glCapabilitiesMet && ASMTransformer.allPatchesSuccessful() && Configurator.enabled;
	}
	
	@SideOnly(Side.CLIENT)
	private static boolean lastEnabledSetting = isModEnabled();
	
	private static boolean glCapabilitiesMet = false;
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
        // check for needed opengl capabilities
        if(!OpenGlHelper.vboSupported)
        {
            getLog().warn(I18n.translateToLocal("misc.fail_no_vbo"));
            return;
        }
        if(!OpenGlHelper.areShadersSupported() )
        {
            getLog().warn(I18n.translateToLocal("misc.fail_no_shaders"));
            return;
        }
        if(!OpenGlHelper.openGL21)
        {
            getLog().warn(I18n.translateToLocal("misc.fail_opengl_version"));
            return;
        }
        getLog().info(I18n.translateToLocal("misc.hardware_ok"));
        glCapabilitiesMet = true;
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
                    AcuityRuntime.INSTANCE.forceReload();
                }});
        }
        
	}
	
    @SideOnly(Side.CLIENT)
	@Mod.EventHandler
    public void imcCallback(FMLInterModComms.IMCEvent event)
	{
        for (FMLInterModComms.IMCMessage message : event.getMessages())
        {
            if (message.key.equalsIgnoreCase("getAcuityRuntime"))
            {
                Optional<Function<IAcuityRuntime, Void>> value = message.getFunctionValue(IAcuityRuntime.class, Void.class);
                if (value.isPresent()) 
                    value.get().apply(AcuityRuntime.INSTANCE);
                else 
                    getLog().warn(I18n.translateToLocal("misc.fail_imc"));
            }
        }
    }
}