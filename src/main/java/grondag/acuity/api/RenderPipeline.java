package grondag.acuity.api;

import grondag.acuity.Configurator;
import grondag.acuity.core.PipelineVertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderPipeline extends Program implements IRenderPipeline
{
    private final int index;
    
    private PipelineVertexFormat pipelineVertexFormat;
    private VertexFormat vertexFormat;
    
    @SuppressWarnings("null")
    RenderPipeline(int index, PipelineVertexShader vertexShader, PipelineFragmentShader fragmentShader, TextureFormat textureFormat)
    {
        super(vertexShader, fragmentShader, textureFormat);
        this.index = index;
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
}
