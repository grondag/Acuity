package grondag.acuity.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import grondag.acuity.Configurator;
import grondag.acuity.api.IPipelineCallback;
import grondag.acuity.api.IProgram;
import grondag.acuity.api.IRenderPipeline;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.core.PipelineVertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
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
    public final TextureFormat textureFormat;
    public final IPipelineCallback callback;
    public final Program program;
    
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

    public void preDraw()
    {
        this.callback.preDraw();
        this.program.activate();
    }
    
    public void postDraw()
    {
        this.callback.postDraw();
    }

    @Override
    public TextureFormat textureFormat()
    {
        return this.textureFormat;
    }
   
}
