package grondag.acuity.buffering;

import org.lwjgl.opengl.GL11;

import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.PipelineVertexFormat;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.opengl.VaoStore;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class SolidDrawableChunkDelegate
{
    private static final int VAO_UNTESTED = -1;
    private static final int VAO_DISABLED = -2;
    
    private final MappedBuffer buffer;
    private final RenderPipeline pipeline;
    final int vertexOffset;
    final int vertexCount;
    /**
     * VAO Buffer name if enabled and initialized.
     */
    int vaoBufferId = VAO_UNTESTED;
    
    public SolidDrawableChunkDelegate(MappedBuffer buffer, RenderPipeline pipeline, int vertexOffset, int vertexCount)
    {
        this.buffer = buffer;
        this.pipeline = pipeline;
        this.vertexCount = vertexCount;
        this.vertexOffset = vertexOffset;
    }
    
    /**
     * Instances that share the same GL buffer will have the same ID.
     * Allows sorting in solid layer to avoid rebinding buffers for draws that
     * will have the same vertex buffer and pipeline/format.
     */
    public int bufferId()
    {
        return this.buffer.glBufferId;
    }
    
    /**
     * The pipeline (and vertex format) associated with this delegate.
     */
    public RenderPipeline getPipeline()
    {
        return this.pipeline;
    }
    
    /**
     * Should only be needed 1X for draws that share the same pipeline and buffer ID.
     */
    public void bind()
    {
        if(this.buffer.isDisposed())
            return;
        
        this.buffer.bindForRender(this);
        if(vaoBufferId > 0)
        {
            OpenGlHelperExt.glBindVertexArray(vaoBufferId);
            return;
        }
        
        if(vaoBufferId == VAO_UNTESTED)
        {
            if(OpenGlHelperExt.isVaoEnabled())
            {
                vaoBufferId = VaoStore.claimVertexArray();
                PipelineVertexFormat format = pipeline.piplineVertexFormat();
                OpenGlHelperExt.glBindVertexArray(vaoBufferId);
                GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                OpenGlHelperExt.enableAttributesVao(format.attributeCount);
                OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), format.stride, 0);
                format.bindAttributeLocations(0);
                return;
            }
            else
                vaoBufferId = VAO_DISABLED;
        }
        
        
    }
    
    /**
     * Assumes pipeline has already been activated and buffer has already been bound via {@link #bind()}
     */
    public void draw()
    {
        if(this.buffer.isDisposed())
            return;
        OpenGlHelperExt.glDrawArraysFast(GL11.GL_QUADS, vertexOffset, vertexCount);
    }
    
    public void release()
    {
        buffer.release(this);
        if(this.vaoBufferId > 0)
        {
            VaoStore.releaseVertexArray(vaoBufferId);
            vaoBufferId = VAO_UNTESTED;
        }
    }

    public void flush()
    {
        this.buffer.flush();
    }
}
