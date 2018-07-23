package grondag.render_hooks.core;

import grondag.render_hooks.api.RenderPipeline;

/**
 * Tracks number of vertices, pipeline and sequence thereof within a buffer.
 */
public class VertexPackingList
{
    private int[] counts = new int[16];
    private RenderPipeline[] pipelines = new RenderPipeline[16];
    
    private int size = 0;
    private int totalBytes = 0;
    
    public void clear()
    {
        this.size = 0;
        this.totalBytes = 0;
    }
    
    public int size()
    {
        return this.size;
    }
    
    public int totalBytes()
    {
        return this.totalBytes;
    }
    
    public void addPacking(RenderPipeline pipeline, int vertexCount)
    {
        if (size == this.pipelines.length)
        {
            final int iCopy[] = new int[size * 2];
            System.arraycopy(this.counts, 0, iCopy, 0, size);
            this.counts  = iCopy;
            
            final RenderPipeline pCopy[] = new RenderPipeline[size * 2];
            System.arraycopy(this.pipelines, 0, pCopy, 0, size);
            this.pipelines  = pCopy;
        }
        this.pipelines[size] = pipeline;
        this.counts[size] = vertexCount;
        this.totalBytes += pipeline.piplineVertexFormat().stride * vertexCount;
        this.size++;
    }
    
    @FunctionalInterface
    public static interface IVertexPackingConsumer
    {
        public void accept(RenderPipeline pipeline, int vertexCount);
    }
    
    public void forEach(IVertexPackingConsumer consumer)
    {
        final int size = this.size;
        if(size == 0) 
            return;
        
        for(int i = 0; i < size; i++)
        {
            consumer.accept(this.pipelines[i], this.counts[i]);
        }
    }
}
