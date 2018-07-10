package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class RenderPipeline
{
    RenderPipeline() {};
    
    /**
     * Called before VBO rendering.</br>
     * 
     * MUST set up vertex bindings. Default setup is for vanilla Block vertex format.<br>
     * Should NOT change matrix, translation or handle buffer binding.</p>
     */
    public void preDraw() {};
    
    /**
     * Called before display list rendering
     */
    public void preDrawList() {};

    /**
     * Called after VBO rendering
     */
    public void postDraw() {};
    
    /**
     * Called after display list rendering
     */
    public void postDrawList() {};

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

}
