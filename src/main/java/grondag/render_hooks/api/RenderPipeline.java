package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import grondag.render_hooks.Configurator;
import grondag.render_hooks.core.PipelineVertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormat;

public final class RenderPipeline implements IRenderPipeline
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
    public final @Nonnull TextureFormat textureFormat;
    public final @Nonnull IPipelineCallback callback;
    public final @Nonnull Program program;
    
    private PipelineVertexFormat pipelineVertexFormat;
    private VertexFormat vertexFormat;
    
    @SuppressWarnings("null")
    RenderPipeline(@Nonnull TextureFormat textureFormat, 
            IProgram program,
            @Nullable IPipelineCallback callback)
    {
        this.textureFormat = textureFormat;
        this.program = (Program)program;
        this.callback = callback == null ? DUMMY_CALLBACK : callback;
        this.refreshVertexFormats();
    }
    
    public void refreshVertexFormats()
    {
        this.pipelineVertexFormat = Configurator.lightingModel.vertexFormat(this.textureFormat);
        this.vertexFormat = this.pipelineVertexFormat.vertexFormat;
    }
    
    public PipelineVertexFormat piplineVertexFormat()
    {
        return this.pipelineVertexFormat;
    }
    
    /**
     * Avoids a pointer chase, more concise code.
     */
    public VertexFormat vertexFormat()
    {
        return this.vertexFormat;
    }

    
    @Override
    public int getIndex()
    {
        return this.index;
    }

    @Override
    public void preDraw()
    {
        this.callback.preDraw();
        this.program.activate();
    }
    
    @Override
    public void postDraw()
    {
        this.callback.postDraw();
    }
   
}
