package grondag.render_hooks;

import grondag.render_hooks.core.LightingModel;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.Config.RequiresMcRestart;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@LangKey("config.general")
@Config(modid = RenderHooks.MODID, type = Type.INSTANCE)
@SideOnly(Side.CLIENT)
public class Configurator
{
    @RequiresMcRestart
    @Comment({"Maximum number of render pipelines that can be registered at runtime.",
        " The value is fixed at startup to enable very fast lookups.",
        " Smaller values will save slightly on memory overhead.  It isn't much but",
        " is configurable for those folks who like to save every byte possible...."})
    @RangeInt(min = 16, max = 1024)
    public static int maxPipelines = 64;
    
    @Comment({"Changing will reload all renderers and models.",
        " Has no effect if mod is disabled because of ASM failures. ",
        " Primary use is for testing. Note that VBOs will be enabled",
        " if mod is enabked, regardless of Minecraft configuration."})
    public static boolean enabled = true;
    
    @Comment({"Lighting model used for rendering.",
        " Changing will reload all renderers and models.",
        " Has no effect if mod is disabled because of ASM failures."})
   public static LightingModel lightingModel = LightingModel.CLASSIC;
}