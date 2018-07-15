package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRenderPipeline
{
    @Nonnull
    PipelineVertexFormat pipelineVertexFormat();

    /**
     * Convenient access to {@link PipelineVertexFormat#vertexFormat}
     */
    @Nonnull
    VertexFormat vertexFormat();
    
    /**
     * For internal use.<br>
     */
    int getIndex();
    
    /**
     * Executes callback if there is one.
     * For internal use.<br>
     */
    void preDraw();
    
    /**
     * Executes callback if there is one.
     * For internal use.<br>
     */
    void postDraw();

}
