package grondag.acuity.buffering;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import grondag.acuity.Acuity;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.opengl.OpenGlHelperExt;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.crash.CrashReport;

public class MappedBufferStore
{
    
    private static final int MIN_CAPACITY = 32;
    private static final int TARGET_BUFFERS = 512;
    
    private static final ArrayBlockingQueue<MappedBuffer> emptyMapped = new ArrayBlockingQueue<MappedBuffer>(TARGET_BUFFERS);
    private static final ConcurrentLinkedQueue<MappedBuffer> emptyUnmapped = new ConcurrentLinkedQueue<>();
    
    /**
     * Buffers that may need defrag and thus need to be mapped.
     */
    private static final ConcurrentLinkedQueue<MappedBuffer> releaseRemapQueue = new ConcurrentLinkedQueue<>();
    
    /**
     * Buffers that have been mapped and are awaiting defrag.
     */
    private static final ArrayBlockingQueue<MappedBuffer> releaseRebufferQueue = new ArrayBlockingQueue<MappedBuffer>(64);
    
    /**
     * Buffers that have been defragged and thus need to be unmapped and reset.
     * New buffers also need to be flushed and swapped for old.
     */
    private static final ConcurrentLinkedQueue<Pair<MappedBuffer, ObjectArrayList<Pair<DrawableChunkDelegate, IMappedBufferDelegate>>>> releaseResetQueue = new ConcurrentLinkedQueue<>();
    
    
    private static final Object[] solidLock = new Object[PipelineManager.MAX_PIPELINES];
    
    private static final MappedBuffer[] solidPartial = new MappedBuffer[PipelineManager.MAX_PIPELINES];
    
    private static final Thread DEFRAG_THREAD;
    private static final Runnable DEFRAGGER = new Runnable()
    {
        @Override
        public void run()
        {
            while(true)
            {
                try
                {
                    MappedBuffer buff = releaseRebufferQueue.poll(27, TimeUnit.DAYS);
                    ObjectArrayList<Pair<DrawableChunkDelegate, IMappedBufferDelegate>> swaps = buff.rebufferRetainers();
                    releaseResetQueue.offer(Pair.of(buff, swaps));
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                catch (Exception e)
                {
                    Acuity.INSTANCE.getLog().error("Unexpected error detected during vertex buffer defrag.", e);;
                }
            }
        }
    };
    
    static
    {
        for(int i = 0; i < PipelineManager.MAX_PIPELINES; i++)
            solidLock[i] = new Object();
        
        DEFRAG_THREAD = new Thread(DEFRAGGER, "Acuity Vertex Buffer Defrag Thread");
        DEFRAG_THREAD.setDaemon(true);
        DEFRAG_THREAD.start();
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
    
    private static void processReleaseRemapQueue()
    {
        if(!releaseRemapQueue.isEmpty())
        {
            while(!releaseRemapQueue.isEmpty() && releaseRebufferQueue.size() < 64)
            {
                MappedBuffer buff = releaseRemapQueue.poll();
                if(buff.retainers.isEmpty())
                    releaseResetQueue.offer(Pair.of(buff, null));
                else
                {
                    buff.bind();
                    buff.map(false);
                    releaseRebufferQueue.offer(buff);
                }
            }
            OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        }
    }
    
    private static void processReleaseResetQueue()
    {
        if(!releaseResetQueue.isEmpty())
        {
            Pair<MappedBuffer, ObjectArrayList<Pair<DrawableChunkDelegate, IMappedBufferDelegate>>> pair = releaseResetQueue.poll();
            
            while(pair != null)
            {
                MappedBuffer buff = pair.getLeft();

                ObjectArrayList<Pair<DrawableChunkDelegate, IMappedBufferDelegate>> list = pair.getRight();
                if(list != null && !list.isEmpty())
                {
                    final int limit = list.size();
                    for(int i = 0; i < limit; i++)
                    {
                        Pair<DrawableChunkDelegate, IMappedBufferDelegate> swap = list.get(i);
                        IMappedBufferDelegate bufferDelegate = swap.getRight();
                        bufferDelegate.flush();
                        swap.getLeft().replaceBufferDelegate(bufferDelegate);
                    }
                    buff.clearRetainers();
                }
                
                buff.reset();
                emptyUnmapped.offer(buff);
                
                pair = releaseResetQueue.poll();
            }
            OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        }
    }
    
    /**
     * Called at start of each render tick from client thread to  
     * maintain a pool of mapped buffers ready for off-thread loading.
     */
    public static void prepareEmpties()
    {
        processReleaseRemapQueue();
       
        processReleaseResetQueue();
        
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
    public static void claimAllocation(RenderPipeline pipeline, int byteCount, Consumer<IMappedBufferDelegate> consumer)
    {
        //PERF - avoid synch
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
        releaseRemapQueue.offer(mappedBuffer);
    }

    public static void forceReload()
    {
        MappedBuffer.inUse.forEach(b -> b.dispose());
        MappedBuffer.inUse.clear();
        emptyMapped.clear();
        emptyUnmapped.clear();
        releaseRebufferQueue.clear();
        releaseRemapQueue.clear();
        for(int i = 0; i < PipelineManager.MAX_PIPELINES; i++)
            solidPartial[i] = null;
        releaseCount = 0;
        statCounter = 0;
    }
    
}
