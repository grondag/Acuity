package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.vertex.VertexFormat;

final class RenderPipelineImpl extends RenderPipeline
{
    private static int nextIndex = 0;
    
    private static final IPipelineCallback DUMMY_CALLBACK = new IPipelineCallback()
    {
        @Override
        public final void preDraw() {}

        @Override
        public final void postDraw(){}
    };
    
    private final int index = nextIndex++;
    private final @Nonnull PipelineVertexFormat pipelineVertexFormat;
    private final @Nonnull VertexFormat vertexFormat;
    private final @Nonnull IPipelineCallback callback;
    private final @Nullable PipelineFragmentShader fragmentShader;
    private final @Nullable PipelineVertexShader vertexShader;
    
    RenderPipelineImpl(@Nonnull PipelineVertexFormat format, 
            @Nullable String vertexShaderFileName, 
            @Nullable String fragmentShaderFileName,
            @Nullable IPipelineCallback callback)
    {
        this.pipelineVertexFormat = format;
        this.vertexFormat = format.vertexFormat;
        this.vertexShader = RenderHookRuntimeImpl.INSTANCE.getShaderManager().getOrCreateVertexShader(vertexShaderFileName);
        this.fragmentShader = RenderHookRuntimeImpl.INSTANCE.getShaderManager().getOrCreateFragmentShader(fragmentShaderFileName);
        this.callback = callback == null ? DUMMY_CALLBACK : callback;
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
    
    @Override
    public final PipelineVertexShader vertexShader()
    {
        return this.vertexShader;
    }

    @Override
    public final PipelineFragmentShader fragmentShader()
    {
        return this.fragmentShader;
    }

    @Override
    public void preDraw()
    {
        this.callback.preDraw();
    }
    
    @Override
    public void postDraw()
    {
        this.callback.postDraw();
    }
   
}
