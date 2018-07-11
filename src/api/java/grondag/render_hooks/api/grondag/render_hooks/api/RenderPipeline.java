package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class RenderPipeline
{
    RenderPipeline() {};
    
    @Nullable
    public abstract PipelineVertexShader vertexShader();
    
    @Nullable
    public abstract PipelineFragmentShader fragmentShader();
    
    @Nonnull
    public abstract PipelineVertexFormat pipelineVertexFormat();

    /**
     * Convenient access to {@link PipelineVertexFormat#vertexFormat}
     */
    @Nonnull
    public abstract VertexFormat vertexFormat();
    
    /**
     * For internal use.<br>
     */
    public abstract int getIndex();
    
    /**
     * Executes callback if there is one.
     * For internal use.<br>
     */
    public abstract void preDraw();
    
    /**
     * Executes callback if there is one.
     * For internal use.<br>
     */
    public abstract void postDraw();

}
