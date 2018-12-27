package grondag.acuity;

import grondag.acuity.core.LightingModel;

public class Configurator
{
//    @LangKey("config.max_pipelines")
//    @RequiresMcRestart
//    @Comment({"Maximum number of render pipelines that can be registered at runtime.",
//        " The value is fixed at startup to enable very fast lookups.",
//        " Smaller values will save slightly on memory overhead.  It isn't much but",
//        " is configurable for those folks who like to save every byte possible...."})
//    @RangeInt(min = 16, max = 1024)
    public static int maxPipelines = 64;
    
//    @LangKey("config.acuity_enabled")
//    @Comment({"Changing will reload all renderers and models.",
//        " Has no effect if mod is disabled because of ASM failures. ",
//        " Primary use is for testing. Note that VBOs will be enabled",
//        " if Acuity is enabled, regardless of Minecraft configuration."})
    public static boolean enabled = true;
    
//    @LangKey("config.acuity_enable_vao")
//    @Comment({"Use Vertex Array Objects if available.",
//        " VAOs generally improve performance when they are supported."})
    public static boolean enable_vao = true;
    
//    @LangKey("config.acuity_fancy_fluids")
//    @Comment({"Enable fancy water and lava rendering.",
//        " This feature is currently work in progress and has no visible effect if enabled."})
    public static boolean fancyFluids = false;
    
//    @LangKey("config.lighting_model")
//    @Comment({"Lighting model used for rendering. (Currently only one is available.)",
//        " Changing will reload all renderers and models.",
//        " Has no effect if Acuity is disabled because of ASM failures."})
    public static LightingModel lightingModel = LightingModel.CLASSIC;

//    @LangKey("config.enable_render_stats")
//    @RequiresMcRestart
//    @Comment({"When enabled, tracks and outputs timing statistics for rendering.",
//        " Has a small performance impact. Useful only for testing."})
    public static boolean enableRenderStats = false;
    
//    @LangKey("config.enable_block_stats")
//    @RequiresMcRestart
//    @Comment({"When enabled, tracks and outputs timing statistics for lighting ",
//        " and buffering block models during chunk rebuilds.",
//        " Has a small performance impact. Useful only for testing."})
    public static boolean enableBlockStats = false;
    
//    @LangKey("config.enable_fluid_stats")
//    @RequiresMcRestart
//    @Comment({"When enabled, tracks and outputs timing statistics for lighting ",
//        " and buffering fluid models during chunk rebuilds.",
//        " Has a small performance impact. Useful only for testing."})
    public static boolean enableFluidStats = false;

//    @LangKey("config.disable_yield")
//    @RequiresMcRestart
//    @Comment({"When enabled, disables the call to Thread.yield() in the main game loop ",
//        " that normally occurs right after display update. The call is probably meant",
//        " to give the OpenGL drivers time to process the command buffer, but in the multi-threaded game ",
//        " Minecraft has become, and with modern drivers, this basically invites other tasks to step on your framerate.",
//        " This patch is purely a performance optimization and is not required for Acuity to operate."})
    public static boolean disableYieldInGameLoop = true;

    // TODO: remove or reimplement
//    public static void handleChange(PostConfigChangedEvent event)
//    {
//        LightingModel oldModel = lightingModel;
//        boolean oldFancyFluids = fancyFluids;
//        boolean oldEnabled = enabled;
//        boolean oldVAO = enable_vao;
//        
//        ConfigManager.sync(Acuity.MODID, Config.Type.INSTANCE);
//        if(oldEnabled != Configurator.enabled)
//        {
//            Acuity.recomputeEnabledStatus();
//            
//            // important to reload renderers immediately in case 
//            // this results in change of vbo to/from  displaylists
//            // or changes rendering pipline logic path
//            Minecraft.getMinecraft().renderGlobal.loadRenderers();
//            
//            final boolean isEnabled = Acuity.isModEnabled();
//            AcuityRuntime.INSTANCE.forEachListener(c -> c.onAcuityStatusChange(isEnabled));
//            
//            // Don't think this is needed because different interface for pipelined models
//            // Minecraft.getMinecraft().refreshResources();
//        }
//        else if (oldModel != Configurator.lightingModel
//                || oldFancyFluids != fancyFluids
//                || oldVAO != enable_vao)
//        {
//            AcuityRuntime.INSTANCE.forceReload();
//            
//            // refresh appearances
//            Minecraft.getMinecraft().renderGlobal.loadRenderers();
//        }
//        
//    }
}