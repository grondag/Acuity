package grondag.acuity.buffering;

import org.lwjgl.opengl.GL11;

import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.PipelineVertexFormat;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.opengl.VaoStore;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class DrawableChunkDelegate
{
    private static final int VAO_UNTESTED = -1;
    private static final int VAO_DISABLED = -2;
    
    private final IMappedBufferReference buffer;
    private final RenderPipeline pipeline;
    final int vertexCount;
    /**
     * VAO Buffer name if enabled and initialized.
     */
    int vaoBufferId = VAO_UNTESTED;
    
    public DrawableChunkDelegate(IMappedBufferReference buffer, RenderPipeline pipeline, int vertexCount)
    {
        this.buffer = buffer;
        this.pipeline = pipeline;
        this.vertexCount = vertexCount;
    }
    
    /**
     * Instances that share the same GL buffer will have the same ID.
     * Allows sorting in solid layer to avoid rebinding buffers for draws that
     * will have the same vertex buffer and pipeline/format.
     */
    public int bufferId()
    {
        return this.buffer.glBufferId();
    }
    
    /**
     * The pipeline (and vertex format) associated with this delegate.
     */
    public RenderPipeline getPipeline()
    {
        return this.pipeline;
    }
    
    /**
     * Won't bind buffer if this buffer same as last - will only do vertex attributes.
     * Returns the buffer Id that is bound, or input if unchanged.
     */
    public int bind(int lastBufferId)
    {
        if(this.buffer.isDisposed())
            return lastBufferId;
        
        if(this.buffer.glBufferId() != lastBufferId)
        {
            this.buffer.bind();
            lastBufferId = this.buffer.glBufferId();
        }
        
        if(vaoBufferId > 0)
        {
            OpenGlHelperExt.glBindVertexArray(vaoBufferId);
            return lastBufferId;
        }
        
        final PipelineVertexFormat format = pipeline.piplineVertexFormat();
        if(vaoBufferId == VAO_UNTESTED)
        {
            if(OpenGlHelperExt.isVaoEnabled())
            {
                vaoBufferId = VaoStore.claimVertexArray();
                OpenGlHelperExt.glBindVertexArray(vaoBufferId);
                GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                OpenGlHelperExt.enableAttributesVao(format.attributeCount);
                bindVertexAttributes(format);
                return lastBufferId;
            }
            else
                vaoBufferId = VAO_DISABLED;
        }
        
        // if get to here, no VAO and must rebind each time
        bindVertexAttributes(format);
        return lastBufferId; 
       
    }
    
    private void bindVertexAttributes(PipelineVertexFormat format)
    {
        OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), format.stride, buffer.byteOffset());
        format.bindAttributeLocations(buffer.byteOffset());
    }
    
    /**
     * Assumes pipeline has already been activated and buffer has already been bound via {@link #bind()}
     */
    public void draw()
    {
        if(this.buffer.isDisposed())
            return;
        OpenGlHelperExt.glDrawArraysFast(GL11.GL_QUADS, 0, vertexCount);
    }
    
    public void release()
    {
        buffer.release();
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
