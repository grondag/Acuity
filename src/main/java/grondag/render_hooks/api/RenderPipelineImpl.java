package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexFormat;

final class RenderPipelineImpl extends RenderPipeline
{
    private static int nextIndex = 0;
    
    final int index = nextIndex++;
    final @Nonnull PipelineVertexFormat pipelineVertexFormat;
    final @Nonnull VertexFormat vertexFormat;
    final @Nullable String vertexShaderFileName;
    final @Nullable String fragmentShaderFileName;
    final @Nullable IPipelineCallback callback;
    
    RenderPipelineImpl(@Nonnull PipelineVertexFormat format, 
            @Nullable String vertexShaderFileName, 
            @Nullable String fragmentShaderFileName,
            @Nullable IPipelineCallback callback)
    {
        this.pipelineVertexFormat = format;
        this.vertexFormat = format.vertexFormat;
        this.vertexShaderFileName = vertexShaderFileName;
        this.fragmentShaderFileName = fragmentShaderFileName;
        this.callback = callback;
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
