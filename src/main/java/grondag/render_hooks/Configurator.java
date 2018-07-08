package grondag.render_hooks;

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
    @Comment({"Maximum number of render pipelines per block layer (SOLID, MIPPED, etc.).",
        " Smaller values will save slightly on memory overhead.  It isn't very much but",
        " is configurable for those folks who like to save every byte possible...."})
    @RangeInt(min = 16, max = 1024)
    public static int maxPipelinesPerRenderLayer = 64;

}