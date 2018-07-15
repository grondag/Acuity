package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.vertex.VertexFormat;

final class RenderPipeline implements IRenderPipeline
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
    private final @Nonnull Program program;
    
    RenderPipeline(@Nonnull PipelineVertexFormat format, 
            IProgram program,
            @Nullable IPipelineCallback callback)
    {
        this.pipelineVertexFormat = format;
        this.vertexFormat = format.vertexFormat;
        this.program = (Program)program;
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
