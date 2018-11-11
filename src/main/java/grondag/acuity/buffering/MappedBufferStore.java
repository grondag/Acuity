package grondag.acuity.buffering;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import grondag.acuity.api.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;

public class MappedBufferStore
{
    private static final int MIN_BUFFERS = 64;
    private static final int MAX_BUFFERS = 128;
    private static int bufferCount = 0;
    private static final MappedBuffer buffers[] = new MappedBuffer[MAX_BUFFERS];
    
    private static final ArrayBlockingQueue<MappedBuffer> freeBuffers = new ArrayBlockingQueue<MappedBuffer>(MAX_BUFFERS);
    
    @SuppressWarnings("unchecked")
    private static final ConcurrentLinkedQueue<BufferAllocation>[][] allocations 
        = new ConcurrentLinkedQueue[TextureFormat.values().length][BufferSlice.SLICE_COUNT];
    
    private static volatile ConcurrentLinkedQueue<BufferAllocation> disposalQueueA = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<BufferAllocation> disposalQueueB = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<BufferAllocation> disposalQueueC = new ConcurrentLinkedQueue<>();
    
    static
    {
        for(ConcurrentLinkedQueue<?>[] array : allocations)
        {
            for(int i = 0; i < BufferSlice.SLICE_COUNT; i++)
                array[i] = new ConcurrentLinkedQueue<BufferAllocation>();
        }
    }
    
    private static MappedBuffer getEmptyMapped()
    {
        try
        {
            return freeBuffers.poll(27, TimeUnit.DAYS);
        }
        catch (Exception e)
        {
            Minecraft.getMinecraft().crashed(new CrashReport("Unable to allocate empty GL buffer", e));
            return buffers[0];
        }
    }
    
    /**
     * Called at start of each render tick from client thread to  
     * maintain a pool of mapped buffers ready for off-thread loading.
     */
    public static void prepareEmpties()
    {
        ConcurrentLinkedQueue<BufferAllocation> disposal = disposalQueueC;
        while(!disposal.isEmpty())
            acceptFreeInner(disposal.poll());
        
        disposalQueueC = disposalQueueB;
        disposalQueueB = disposalQueueA;
        disposalQueueA = disposal;
        
        final int targetBuffers = Math.max(freeBuffers.size() - 4, MIN_BUFFERS - bufferCount);
        
        while(freeBuffers.size() < targetBuffers && bufferCount < MAX_BUFFERS)
        {
            MappedBuffer empty =  new MappedBuffer();
            buffers[bufferCount++] = empty;
            freeBuffers.offer(empty);
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
//            final int extantCount = MappedBuffer.inUse.size();
//            MappedBuffer.inUse.forEach(b -> b.reportStats());
//            Acuity.INSTANCE.getLog().info("Extant Mapped Buffers: " + extantCount);
//            Acuity.INSTANCE.getLog().info("Extant Mapped Capacity (MB): " + extantCount * MappedBuffer.CAPACITY_BYTES / 0x100000);
//            Acuity.INSTANCE.getLog().info("Ready Buffers: " + emptyMapped.size());
//            Acuity.INSTANCE.getLog().info("Idle Buffers: " + emptyUnmapped.size());
//            Acuity.INSTANCE.getLog().info("Release Count (Lifetime): " + releaseCount);
//            Acuity.INSTANCE.getLog().info("");
        }
    }
    
    /**
     * Will give consumer one or more buffers w/ offsets able to contain the given byte count.
     * If more than one buffer is needed, break(s) will be at a boundary compatible with all vertex formats.
     * All vertices in the buffer(s) will share the same pipeline (and thus vertex format).
     */
    @SuppressWarnings("null")
    public static void claimAllocation(TextureFormat format, int quadCount, Consumer<IBufferAllocation> consumer)
    {
        BufferAllocator allocator = BufferAllocator.findBest(format, quadCount);
        assert allocator.quadCount >= quadCount;
        
        if(allocator.isDouble)
        {
            assert quadCount - allocator.primarySlice.quadCount > 0;
            consumer.accept(getAllocation(allocator.primarySlice, allocator.primarySlice.quadCount));
            consumer.accept(getAllocation(allocator.secondarySlice, quadCount - allocator.primarySlice.quadCount));
        }
        else
        {
            assert quadCount <= allocator.primarySlice.quadCount;
            consumer.accept(getAllocation(allocator.primarySlice, quadCount));
        }
    }

    private static BufferAllocation getAllocation(BufferSlice slice, int quadCount)
    {
        BufferAllocation a = getAllocation(slice);
        a.setQuadCount(quadCount);
        return a;
    }
    
    /**
     * Returned allocation will be claimed.
     */
    private static BufferAllocation getAllocation(BufferSlice slice)
    {
        ConcurrentLinkedQueue<BufferAllocation> q = allocations[slice.formatOrdinal][slice.divisionLevel];
        BufferAllocation a = q.poll();
        while(a != null && !a.claim())
            a = q.poll();
        return a == null ? newAllocation(slice) : a;
    }
    
    /**
     * Returned allocation will be claimed.
     */
    private static BufferAllocation newAllocation(BufferSlice slice)
    {
        if(slice.isMax)
        {
            BufferAllocation result = new BufferAllocation.Root(slice, getEmptyMapped());
            result.buffer().setFormat(slice.format);
            result.claim();
            return result;
        }
        else
        {
            @SuppressWarnings("null")
            BufferAllocation parent = getAllocation(slice.bigger());
            BufferAllocation.Slice a = new BufferAllocation.Slice(slice, parent.startVertex(), parent);
            BufferAllocation.Slice b = new BufferAllocation.Slice(slice, parent.startVertex() + slice.quadCount * 4, parent);
            a.buddy = b;
            b.buddy = a;
            a.claim();
            assert a.isFree.get() == false;
            MappedBufferStore.acceptFree(b);
            return a;
        }
    }

    public static void forceReload()
    {
        for(int i = 0; i < bufferCount; i++)
        {
            buffers[i].dispose();
            buffers[i] = null;
        }
        bufferCount = 0;
        
        disposalQueueA.clear();
        disposalQueueB.clear();
        freeBuffers.clear();
        
        for(ConcurrentLinkedQueue<?>[] array : allocations)
        {
            for(ConcurrentLinkedQueue<?> q : array)
            {
                q.clear();
            }
        }
        releaseCount = 0;
        statCounter = 0;
    }

    /**
     * Saves free allocation for reuse.
     */
    static void acceptFreeInner(BufferAllocation free)
    {
        ConcurrentLinkedQueue<BufferAllocation> q = allocations[free.slice.formatOrdinal][free.slice.divisionLevel];
        if(free instanceof BufferAllocation.Root)
        {
            if(q.isEmpty())
                q.offer(free);
            else
            {
                free.isDeleted = true;
                freeBuffers.offer(free.buffer());
            }
        }
        else
            q.offer(free);
    }
    
    static void acceptFree(BufferAllocation free)
    {
        disposalQueueA.offer(free);
    }
}
