package grondag.acuity.api;

import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Configurator;
import grondag.acuity.core.PipelineFragmentShader;
import grondag.acuity.core.PipelineVertexFormat;
import grondag.acuity.core.PipelineVertexShader;
import grondag.acuity.core.Program;
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
    
    @Override
    public void forceReload()
    {
        super.forceReload();
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
     * Will update modelView and modelViewProjection matrix uniforms if they were created via {@link #setupModelViewUniforms()}.
     * Otherwise has no effect.  Should be called after it is known the projection matrix will be current.<p>
     * 
     * Returns self as convenience.
     */
    @SuppressWarnings("null")
    public RenderPipeline updateModelViewMatrix(Matrix4f modelViewMatrix)
    {
        if(this.modelViewUniform != null);
            this.modelViewUniform.set(modelViewMatrix);
            
        if(this.modelViewProjectionUniform != null);
            this.modelViewProjectionUniform.set(Matrix4f.mul(modelViewMatrix, PipelineManager.INSTANCE.projMatrix, null));
            
        return this;
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
