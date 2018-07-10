package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import grondag.render_hooks.Configurator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
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
    public abstract RenderPipeline createPipeline(@Nonnull PipelineVertexFormat format);
    
    /**
     * Use when BLOCK vertex format is enough and you want to mimic MC rendering.
     */
    public abstract RenderPipeline getVanillaPipeline();
    
    /**
     * Use this to retrieve the vertex format that is generated for the given pipeline.
     * The same instance will be returned for pipelines with the same format
     * allowing for efficient comparison and sorting by the API.<p>
     * 
     * Will be {@link DefaultVertexFormats#BLOCK} unless the pipeline overrides
     * {@link RenderPipeline#enableNormalVertexExtension()}, {@link RenderPipeline#colorVertexExtensions()}
     * or IRenderPipeline#
     */
    public abstract VertexFormat getVertexFormat(RenderPipeline pipeline);
    
}
