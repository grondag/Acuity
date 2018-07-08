package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import grondag.render_hooks.Configurator;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelineManager
{
    public static final int MAX_PIPELINES_PER_RENDER_LAYER = Configurator.maxPipelinesPerRenderLayer;
    
    /**
     * Will always be 0, defined to clarify intent in code.
     */
    public static final int VANILLA_MC_PIPELINE_INDEX = 0;
    
    /**
     * Will always be 1, defined to clarify intent in code.
     */
    public static final int FIRST_CUSTOM_PIPELINE_INDEX = 1;
    
    /**
     * Will return false if pipeline limit would be exceeded.
     */
    public boolean createPipeline(@Nonnull IRenderPipeline material);
    
    public IRenderPipeline getVanillaPipeline(@Nonnull BlockRenderLayer forLayer);
}
