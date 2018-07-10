package grondag.render_hooks.api;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexFormat;

public final class RenderPipelineImpl extends RenderPipeline
{
    private static int nextIndex = 0;

    public static final RenderPipeline VANILLA_PIPELINE = new RenderPipelineImpl(PipelineVertexFormat.BASE);
    
    final int index = nextIndex++;
    final PipelineVertexFormat pipelineVertexFormat;
    final VertexFormat vertexFormat;
    
    RenderPipelineImpl(PipelineVertexFormat vertexFormat)
    {
        this.pipelineVertexFormat = vertexFormat;
        this.vertexFormat = vertexFormat.vertexFormat;
    }
    
    @Override
    public final PipelineVertexFormat pipelineVertexFormat()
    {
        return this.pipelineVertexFormat;
    }

    @Override
    public final VertexFormat vertexFormat()
    {
        return this.vertexFormat;
    }
    
    @Override
    public int getIndex()
    {
        return this.index;
    }
    
    //TODO: separate VBO path 
    //TODO: list and VBO paths default to generic pre-post
    @Override
    public void preDraw()
    {
        //TODO: do this based on vertex format
        GlStateManager.glVertexPointer(3, 5126, 28, 0);
        GlStateManager.glColorPointer(4, 5121, 28, 12);
        GlStateManager.glTexCoordPointer(2, 5126, 28, 16);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glTexCoordPointer(2, 5122, 28, 24);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}
