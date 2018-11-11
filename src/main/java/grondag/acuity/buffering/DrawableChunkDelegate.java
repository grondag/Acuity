package grondag.acuity.buffering;

import org.lwjgl.opengl.GL11;

import grondag.acuity.api.RenderPipeline;
import grondag.acuity.opengl.OpenGlHelperExt;

public class DrawableChunkDelegate
{
    private final BufferAllocation buffer;
    private final RenderPipeline pipeline;
    final int vertexCount;
    
    public DrawableChunkDelegate(BufferAllocation buffer, RenderPipeline pipeline, int vertexCount)
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
       
        return lastBufferId; 
    }
    
    /**
     * Assumes pipeline has already been activated and buffer has already been bound via {@link #bind()}
     */
    public void draw()
    {
        if(buffer.isDisposed())
            return;
        buffer.bindVertexAttributes();
        OpenGlHelperExt.glDrawArraysFast(GL11.GL_QUADS, 0, vertexCount);
        buffer.fence.set();
    }
    
    public void release()
    {
        buffer.release();
    }

    public void flush()
    {
        this.buffer.flush();
    }

    public void flushLater()
    {
        this.buffer.flushLater();
    }
}
