package grondag.acuity.buffering;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.Acuity;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;

public class MappedBufferStore
{
    private static final int MIN_CAPACITY = 32;
    private static final int TARGET_BUFFERS = 512;
    
    private static final ArrayBlockingQueue<MappedBuffer> emptyMapped = new ArrayBlockingQueue<MappedBuffer>(TARGET_BUFFERS);
    private static final ConcurrentLinkedQueue<MappedBuffer> emptyUnmapped = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<MappedBuffer> pendingRelease = new ConcurrentLinkedQueue<>();
    
    private static final Object[] solidLock = new Object[PipelineManager.MAX_PIPELINES];
    
    private static final MappedBuffer[] solidPartial = new MappedBuffer[PipelineManager.MAX_PIPELINES];
    
    static
    {
        for(int i = 0; i < PipelineManager.MAX_PIPELINES; i++)
            solidLock[i] = new Object();
    }
    
    private static @Nullable MappedBuffer getEmptyMapped()
    {
        try
        {
            return emptyMapped.poll(27, TimeUnit.DAYS);
        }
        catch (Exception e)
        {
            Minecraft.getMinecraft().crashed(new CrashReport("Unable to allocate empty GL buffer", e));
            return null;
        }
    }
    
    /**
     * Called at start of each render tick from client thread to  
     * maintain a pool of mapped buffers ready for off-thread loading.
     */
    public static void prepareEmpties()
    {
//        while(!pendingRelease.isEmpty())
//        {
//            MappedBuffer b = pendingRelease.poll();
//            b.reset();
//            if(!b.isDisposed())
//                emptyUnmapped.offer(b);
//        }
        
        final int targetBuffers = Math.max(MIN_CAPACITY, TARGET_BUFFERS - MappedBuffer.inUse.size());
        
        while(emptyMapped.size() < targetBuffers)
        {
            MappedBuffer empty =  emptyUnmapped.poll();

            if(empty == null)
                empty = new MappedBuffer();
            else
                empty.remap();
            
            emptyMapped.offer(empty);
        }
        
        doStats();
    }
    
    //TODO: disable
    
    static int statCounter = 0;
    static int releaseCount = 0;
    
    private static void doStats()
    {
        if(statCounter++ == 2400)
        {
            statCounter = 0;
            final int extantCount = MappedBuffer.inUse.size();
            MappedBuffer.inUse.forEach(b -> b.reportStats());
            Acuity.INSTANCE.getLog().info("Extant Mapped Buffers: " + extantCount);
            Acuity.INSTANCE.getLog().info("Extant Mapped Capacity (MB): " + extantCount * MappedBuffer.CAPACITY_BYTES / 0x100000);
            Acuity.INSTANCE.getLog().info("Ready Buffers: " + emptyMapped.size());
            Acuity.INSTANCE.getLog().info("Idle Buffers: " + emptyUnmapped.size());
            Acuity.INSTANCE.getLog().info("Release Count (Lifetime): " + releaseCount);
            Acuity.INSTANCE.getLog().info("");
        }
    }
    
    /**
     * Will give consumer one or more buffers w/ offsets able to contain the given byte count.
     * If more than one buffer is needed, break(s) will be at a boundary compatible with all vertex formats.
     * All vertices in the buffer(s) will share the same pipeline (and thus vertex format).
     */
    public static void claimSolid(RenderPipeline pipeline, int byteCount, Consumer<IMappedBufferDelegate> consumer)
    {
        synchronized(solidLock[pipeline.getIndex()])
        {
            final MappedBuffer startBuffer = solidPartial[pipeline.getIndex()];
            MappedBuffer target =  startBuffer;
            
            if(target == null)
                target = getEmptyMapped();
            
            if(target == null)
                return;
            
            final int quadStride = pipeline.piplineVertexFormat().stride * 4;
                
            while(byteCount > 0)
            {
                IMappedBufferDelegate result = target.requestBytes(byteCount, quadStride);
                if(result == null)
                {
                    // store no longer knows/cares about it, and it can be released when no longer needed for render
                    target.setFinal();
                    target = getEmptyMapped();
                    if(target == null)
                        return;
                }
                else
                {
                    consumer.accept(result);
                    byteCount -= result.byteCount();
                }
            }
            
            if(startBuffer != target)
                solidPartial[pipeline.getIndex()] = target;
        }
    }
    
    /**
     * Called by mapped buffers when they are released off thread.
     * Prevents GL calls outside client thread.
     */
    public static void scheduleRelease(MappedBuffer mappedBuffer)
    {
        pendingRelease.offer(mappedBuffer);
    }

    public static void forceReload()
    {
        MappedBuffer.inUse.forEach(b -> b.dispose());
        MappedBuffer.inUse.clear();
        emptyMapped.clear();
        emptyUnmapped.clear();
        pendingRelease.clear();
        for(int i = 0; i < PipelineManager.MAX_PIPELINES; i++)
            solidPartial[i] = null;
        releaseCount = 0;
        statCounter = 0;
    }
    
}
