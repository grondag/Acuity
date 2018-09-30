package grondag.acuity.core;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;

public class VertexCollectorList
{
    /**
     * Cache instantiated buffers for reuse.<p>
     */
    private static final ConcurrentLinkedQueue<VertexCollectorList> lists = new ConcurrentLinkedQueue<>();

    public static VertexCollectorList claim()
    {
        VertexCollectorList result = lists.poll();
        if(result == null)
            result = new VertexCollectorList();
        return result;
    }
    
    public static void release(VertexCollectorList list)
    {
        list.releaseCollectors();
        lists.offer(list);
    }
    
    /**
     * Fast lookup of buffers by pipeline index. Null in CUTOUT layer buffers.
     */
    private VertexCollector[] vertexCollectors  = new VertexCollector[PipelineManager.MAX_PIPELINES];
    
    private int maxIndex = -1;
    
    private VertexCollectorList()
    {
        //
    }
    
    private void releaseCollectors()
    {
        final int limit = maxIndex;
        if(limit == -1) 
            return;
            
        maxIndex = -1;
        for(int i = 0; i <= limit; i++)
        {
            VertexCollector vc = vertexCollectors[i];
            if(vc == null)
                continue;
            VertexCollector.release(vc);
            vertexCollectors[i] = null;
        }
    }
    
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        releaseCollectors();
    }

    public final boolean isEmpty()
    {
        return this.maxIndex == -1;
    }
    
    public final VertexCollector getIfExists(final int pipelineIndex)
    {
        return vertexCollectors[pipelineIndex];
    }
    
    public final VertexCollector getIfExists(RenderPipeline pipeline)
    {
        return vertexCollectors[pipeline.getIndex()];
    }
    
    public final VertexCollector getOrCreate(RenderPipeline pipeline)
    {
        final int i = pipeline.getIndex();
        VertexCollector result = vertexCollectors[i];
        if(result == null)
        {
            if(i > maxIndex)
                maxIndex = i;
            
            result = VertexCollector.claimAndPrepare(pipeline);
            vertexCollectors[i] = result;
        }
        return result;
    }
    
    public final void forEachExisting(Consumer<VertexCollector> consumer)
    {
        final int limit = maxIndex;
        if(limit == -1) 
            return;
            
        for(int i = 0; i <= limit; i++)
        {
            VertexCollector vc = vertexCollectors[i];
            if(vc == null)
                continue;
            consumer.accept(vc);
        }
    }
}
