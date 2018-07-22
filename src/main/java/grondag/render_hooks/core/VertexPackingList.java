package grondag.render_hooks.core;

import grondag.render_hooks.api.RenderPipeline;

/**
 * Tracks byte offets, number of vertices, and pipeline within a buffer.
 */
public class VertexPackingList
{
    private int[] countsAndOffsets = new int[32];
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
            final int iCopy[] = new int[size * 4];
            System.arraycopy(this.countsAndOffsets, 0, iCopy, 0, size * 2);
            this.countsAndOffsets  = iCopy;
            
            final RenderPipeline pCopy[] = new RenderPipeline[size * 2];
            System.arraycopy(this.pipelines, 0, pCopy, 0, size);
            this.pipelines  = pCopy;
        }
        this.pipelines[size] = pipeline;
        final int index = size++ * 2;
        this.countsAndOffsets[index] = vertexCount;
        this.countsAndOffsets[index + 1] = this.totalBytes;
        this.totalBytes += pipeline.piplineVertexFormat().stride * vertexCount;
    }
    
    @FunctionalInterface
    public static interface IListConsumer
    {
        public void accept(RenderPipeline pipeline, int byteOffset, int vertexCount);
    }
    
    public void forEach(IListConsumer consumer)
    {
        final int size = this.size;
        if(size == 0) 
            return;
        
        for(int i = 0; i < size; i++)
        {
            final int j = i * 2;
            consumer.accept(this.pipelines[i], this.countsAndOffsets[j + 1], this.countsAndOffsets[j]);
        }
    }
}
