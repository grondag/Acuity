package grondag.acuity.buffering;

import grondag.acuity.api.RenderPipeline;

public interface IDrawableBufferDelegate
{
    /**
     * Instances that share the same GL buffer will have the same ID.
     * Allows sorting in solid layer to avoid rebinding buffers for draws that
     * will have the same vertex buffer and pipeline/format.
     */
    public int bufferId();
    
    /**
     * The pipeline (and vertex format) associated with this delegate.
     */
    public RenderPipeline getPipeline();
    
    /**
     * Should only be needed 1X for draws that share the same pipeline and buffer ID.
     */
    public void bind();
    
    /**
     * Assumes pipeline has already been activated and buffer has already been bound via {@link #bind()}
     */
    public void draw();
}
