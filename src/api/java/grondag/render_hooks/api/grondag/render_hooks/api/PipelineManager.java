package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import grondag.render_hooks.Configurator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class PipelineManager
{
    PipelineManager() { }
    
    public static final int MAX_PIPELINES = Configurator.maxPipelines;
    
    /**
     * Will always be 0, defined to clarify intent in code.
     */
    public static final int VANILLA_MC_PIPELINE_INDEX = 0;
    
    /**
     * Will always be 1, defined to clarify intent in code.
     */
    public static final int FIRST_CUSTOM_PIPELINE_INDEX = 1;
    
    /**
     * Will return null if pipeline limit would be exceeded.
     */
    @Nullable
    public abstract RenderPipeline getOrCreatePipeline(
            @Nonnull PipelineVertexFormat format, 
            @Nullable String vertexShaderFileName, 
            @Nullable String fragmentShaderFileName,
            @Nullable IPipelineCallback callback);  
    /**
     * Use when BLOCK vertex format is enough and you want to mimic MC rendering.
     */
    @Nonnull
    public abstract RenderPipeline getVanillaPipeline();
    
}
