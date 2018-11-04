package grondag.acuity.buffering;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;

public class MappedBufferStore
{
    private static final int CAPACITY = 32;
    private static final ArrayBlockingQueue<MappedBuffer> emptyMapped = new ArrayBlockingQueue<MappedBuffer>(CAPACITY);
    private static final ConcurrentLinkedQueue<MappedBuffer> emptyUnmapped = new ConcurrentLinkedQueue<MappedBuffer>();
    
    private static final Object[] solidLock = new Object[PipelineManager.MAX_PIPELINES];
    private static final Object translucentLock = new Object();
    
    private static final MappedBuffer[] solidPartial = new MappedBuffer[PipelineManager.MAX_PIPELINES];
    private static @Nullable MappedBuffer translucentPartial = null;
    
    static
    {
        for(int i = 0; i < PipelineManager.MAX_PIPELINES; i++)
            solidLock[i] = new Object();
    }
    
    @SuppressWarnings("null")
    private static MappedBuffer getEmptyMapped()
    {
        try
        {
            return emptyMapped.poll(27, TimeUnit.DAYS);
        }
        catch (InterruptedException e)
        {
            //UGLY: what to do.... crash better?
            return null;
        }
    }
    
    /**
     * Called at start of each render tick from client thread to  
     * maintain a pool of mapped buffers ready for off-thread loading.
     */
    public static void prepareEmpties()
    {
        while(emptyMapped.size() < CAPACITY)
        {
            MappedBuffer empty =  emptyUnmapped.poll();

            if(empty == null)
                empty = new MappedBuffer();
            else
                empty.remap();
            
            // prevent buffers still being filled from being released when the only chunk using it is rebuilt
            empty.retain();
            
            emptyMapped.offer(empty);
        }
    }
    
    /**
     * Will give consumer one or more buffers w/ offsets able to contain the given byte count.
     * If more than one buffer is needed, break(s) will be at a boundary compatible with all vertex formats.
     * All vertices in the buffer(s) will share the same pipeline (and thus vertex format).
     */
    public static void claimSolid(RenderPipeline pipeline, int byteCount, IBufferConsumer consumer)
    {
        synchronized(solidLock[pipeline.getIndex()])
        {
            final MappedBuffer startBuffer = solidPartial[pipeline.getIndex()];
            MappedBuffer target =  startBuffer;
            
            if(target == null)
                target = getEmptyMapped();
                
            while(byteCount > 0)
            {
                long result = target.requestBytes(byteCount, pipeline.piplineVertexFormat().stride);
                if(result == 0)
                {
                    // store no longer knows/cares about it, and it can be released when no longer needed for render
                    target.release();
                    target = getEmptyMapped();
                }
                else
                {
                    final int filled = (int) (result >> 32);
                    consumer.accept((int)(result & 0xFFFFFFFF), filled, target);
                    byteCount -= filled;
                }
            }
            
            if(startBuffer != target)
                solidPartial[pipeline.getIndex()] = target;
        }
    }

    /**
     * Will give consumer one or more buffers w/ offsets able to contain the given byte count.
     * If more than one buffer is needed, break will be at a boundary compatible with all vertex formats.
     * Unlike {@link #claimSolid(RenderPipeline, int, int, IBufferConsumer)} this assumes all pipelines
     * and potentially multiple vertex formats will be backed into the same space to honor vertex sorting.
     * Will not split across buffers.
     */
    public static void claimTranslucent(int byteCount, IBufferConsumer consumer)
    {
        synchronized(translucentLock)
        {
            final MappedBuffer startBuffer = translucentPartial;
            MappedBuffer target =  startBuffer;
            
            if(target == null)
                target = getEmptyMapped();
                
            int offset = target.requestBytes(byteCount);
            if(offset == MappedBuffer.UNABLE_TO_ALLOCATE)
            {
                // store no longer knows/cares about it, and it can be released when no longer needed for render
                target.release();
                target = getEmptyMapped();
                offset = target.requestBytes(byteCount);
            }
            
            assert offset != MappedBuffer.UNABLE_TO_ALLOCATE;
            
            if(offset != MappedBuffer.UNABLE_TO_ALLOCATE)
                consumer.accept(offset, byteCount, target);
                
            if(startBuffer != target)
                translucentPartial = target;
        }
    }
    
    /**
     * To be called only from MappedBuffer
     */
    static void release(MappedBuffer buffer)
    {
        emptyUnmapped.offer(buffer);
    }
    
}