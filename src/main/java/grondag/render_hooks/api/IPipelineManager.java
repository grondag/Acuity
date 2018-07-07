package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelineManager
{
    public static final int MAX_PIPELINE_COUNT = 64;
    
    public static final int PIPELINE_VANILLA_SOLID = BlockRenderLayer.SOLID.ordinal();
    public static final int PIPELINE_VANILLA_CUTOUT = BlockRenderLayer.CUTOUT.ordinal();
    public static final int PIPELINE_VANILLA_CUTOUT_MIPPED = BlockRenderLayer.CUTOUT_MIPPED.ordinal();
    public static final int PIPELINE_VANILLA_TRANSLUCENT = BlockRenderLayer.TRANSLUCENT.ordinal();
    
    
    /**
     * Will return false if pipeline limit would be exceeded.
     */
    public boolean createPipeline(@Nonnull IRenderPipeline material);
}
