package grondag.acuity.buffering;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL15;

import grondag.acuity.Acuity;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.opengl.GLBufferStore;
import grondag.acuity.opengl.OpenGlHelperExt;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

/**
 * Concurrency Notes<br>
 * Client thread must handle all operations that require GL context.
 * This include map, flush, bind, etc.<p>
 * 
 * Allocation operations will occur on chunk build thread.
 * An external "check-in/out" mechanism will ensure only a single
 * thread is ever calling these operations.<p>
 * 
 * Some allocation operations may interact with tracking data needed on the client thread.
 * For these, Atomic variable are preferred over synchronization.
 *
 */
public class MappedBuffer
{
    private static int nextID = 0;
    
    public static final int CAPACITY_BYTES = 512 * 1024;
    private static final int HALF_CAPACITY = CAPACITY_BYTES / 2;
    
    public static final ObjectArrayList<MappedBuffer> inUse = new ObjectArrayList<>();
    
    public final int glBufferId;
    
    public final int id = nextID++;
    
    /**
     * Max byte used for buffer flush. Incremented by allocation 
     * on chunk build threads and decreased by flush on client threads.
     */
    private final AtomicInteger currentMaxOffset = new AtomicInteger();
    
    /**
     * Max byte flushed to GPU. Changed by client thread only.
     */
    private int lastFlushedOffset = 0;
    
    /**
     * Mapped GL buffer if currently mapped. Set by client thread only
     * but access by other threads is the point of having it.<p>
     * 
     * MAY BE NON-NULL IF NOT MAPPED.  Reuse of buffer instance is an optimization feature of GL.
     */
    private @Nullable ByteBuffer mapped = null;
    
    /**
     * True if buffer is actually mapped. If false, and {@link #mapped} is non-null,
     * the buffer instance is only meant for remap calls. Modified by client thread only.
     */
    private boolean isMapped = false;
    
    /**
     * If true, buffer is fully allocated and does not need to be re-mapped after flush.
     * Changed by allocation thread - which will always be a single thread.
     */
    private volatile boolean isFinal = false;
    
    
    private boolean isMappedReadonly = false;
    
    /**
     * Bytes currently being used for render. 
     */
    private final AtomicInteger retainedBytes = new AtomicInteger();
    
    /**
     * If true, release (or defrag) has been requested because buffer is final
     * and usage has dropped below 50%.  *Probably* only updated on client thread
     * but handled as atomic just be sure. 
     */
    private final AtomicBoolean isReleaseRequested = new AtomicBoolean(false);
    
    /**
     * DrawableChunkDelegates currently using the buffer for rendering.
     */
    final Set<DrawableChunkDelegate> retainers = Collections.newSetFromMap(new ConcurrentHashMap<DrawableChunkDelegate, Boolean>());
    
    MappedBuffer()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        this.glBufferId = OpenGlHelper.glGenBuffers();
        bind();
        orphan();
        map(true);
        unbind();
        inUse.add(this);
    }
    
    public ByteBuffer byteBuffer()
    {
        assert mapped != null;
        return mapped;
    }
    
    void map(boolean writeFlag)
    {
        if(isDisposed)
            return;
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        mapped = OpenGlHelperExt.mapBufferAsynch(mapped, CAPACITY_BYTES, writeFlag);
        isMapped = true;
        isMappedReadonly = !writeFlag;
    }
    
    /** Called for buffers that are being reused.  Should already have been orphaned earlier.*/
    public void remap()
    {
        if(isDisposed)
            return;
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        bind();
        map(true);
        unbind();
    }
    
    public int unallocatedBytes()
    {
        return isFinal ? 0 : (CAPACITY_BYTES - currentMaxOffset.get());
    }
    
    /**
     * Won't break stride.
     */
    public @Nullable IMappedBufferDelegate requestBytes(int byteCount, int stride)
    {
        assert !isDisposed;
        assert mapped != null;
        
        int oldOffset, newOffset, filled;
        while(true)
        {
            oldOffset = currentMaxOffset.get();
            
            filled = ((CAPACITY_BYTES - oldOffset) / stride) * stride;
            
            if(filled <= 0)
                return null;
            
            if(filled > byteCount)
                filled = byteCount;
            
            newOffset = oldOffset + filled;
            
            if(currentMaxOffset.compareAndSet(oldOffset, newOffset))
                return new SimpleMappedBufferReference(this, oldOffset, filled);
        }
    }

    void bind()
    {
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
    }
    
    private void unbind()
    {
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }

    /** assumes buffer is bound */
    private void orphan()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        OpenGlHelperExt.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, CAPACITY_BYTES, GL15.GL_STATIC_DRAW);
        OpenGlHelperExt.handleAppleMappedBuffer();
    }
    
    public boolean isFlushPending()
    {
        return isMapped && lastFlushedOffset != currentMaxOffset.get();
    }
    
    /**
     * Called each tick to send updates to GPU. 
     * TODO: locking to prevent content upload during flush.
     */
    public void flush()
    {
        if(isDisposed)
            return;
        
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        
        final int currentMax = currentMaxOffset.get();
        final int bytes = currentMax - lastFlushedOffset;
        
        if(bytes == 0 && !isMapped)
            return;
        
        bind();
        if(bytes != 0)
        {
            assert isMapped;
            assert !isMappedReadonly;
            
            OpenGlHelperExt.flushBuffer(lastFlushedOffset, bytes);
            lastFlushedOffset = currentMax;
        }
        
        if(isMapped)
        {
            OpenGlHelperExt.unmapBuffer();
            
            // need to check again because another thread could have set us final or add vertices after we started
            if(isFinal && currentMaxOffset.get() == lastFlushedOffset)
            {
                isMapped = false;
                mapped = null;
            }
            else
                remap();
        }
        
        unbind();
    }
    
    /** called to leave buffer unmapped on next synch when will no longer be adding */
    public void setFinal()
    {
        this.isFinal = true;
    }

    /**
     * Called implicitly when bytes are allocated.
     * Store calls explicitly to retain while this buffer is being filled.
     */
    public void retain(DrawableChunkDelegate drawable)
    {
        retainedBytes.addAndGet(drawable.bufferDelegate().byteCount());
        retainers.add(drawable);
    }
    
    public void release(DrawableChunkDelegate drawable)
    {
        // retainer won't be found if release was already scheduled and collection has been cleared
        if(retainers.remove(drawable))
        {
            final int bytes = drawable.bufferDelegate().byteCount();
            final int newRetained = retainedBytes.addAndGet(-bytes);
            if(newRetained < HALF_CAPACITY && isFinal && isReleaseRequested.compareAndSet(false, true))
            {
                MappedBufferStore.scheduleRelease(this);
            }
        }
        else
        {
            // if not found, then should have been cleared after requesting release
            assert isReleaseRequested.get();
            assert retainers.isEmpty();
        }
        
    }
    
    public void reportStats()
    {
        String status = "" + (this.isFinal ? "FINAL" : "OPEN") + (this.isMapped ? "-MAPPED" : "") + (this.isReleaseRequested.get() ? "-RELEASING" : "");
        Acuity.INSTANCE.getLog().info(String.format("Buffer %d: Count=%d  Bytes=%d (%d) Status=%s",
                this.id, 
                this.retainers.size(), 
                this.retainedBytes.get(),
                this.retainedBytes.get() * 100 / CAPACITY_BYTES,
                status));
    }
    
    /** called by store on render reload to recycle GL buffer */
    void dispose()
    {
        if(isMapped)
        {
            bind();
            OpenGlHelperExt.unmapBuffer();
            unbind();
            isMapped = false;
            mapped = null;
        }
        
        if(!isDisposed)
        {
            isDisposed = true;
            GLBufferStore.releaseBuffer(glBufferId);
        }
    }
    
    private boolean isDisposed = false;
    
    public boolean isDisposed()
    {
        return isDisposed;
    }
    
    public void reset()
    {
        assert retainedBytes.get() == 0;
        assert retainers.isEmpty();
        
        bind();
        if(isMapped)
        {
            OpenGlHelperExt.unmapBuffer();
            isMapped = false;
            mapped = null;
            isMappedReadonly = false;
        }
        orphan();
        isFinal = false;
        isReleaseRequested.set(false);
        currentMaxOffset.set(0);
        lastFlushedOffset = 0;
    }

    private static final ThreadLocal<int[]> transferArray = new ThreadLocal<int[]>()
    {
        @Override
        protected int[] initialValue()
        {
            return new int[CAPACITY_BYTES / 4];
        }
    };
    
    public ObjectArrayList<Pair<DrawableChunkDelegate, IMappedBufferDelegate>> rebufferRetainers()
    {
        if(isDisposed)
            return null;
        
        assert isMapped;
        assert isMappedReadonly;
        assert mapped != null;
        
        final IntBuffer fromBuffer = mapped.asIntBuffer();
        final int[] transfer = transferArray.get();
        ObjectArrayList<Pair<DrawableChunkDelegate, IMappedBufferDelegate>> swaps = new ObjectArrayList<>();
        
        retainers.forEach(delegate -> 
        {
            final RenderPipeline pipeline = delegate.getPipeline();
            final int fromByteCount = delegate.bufferDelegate().byteCount();
            final int fromIntCount = fromByteCount / 4;
            final int fromIntOffset = delegate.bufferDelegate().byteOffset() / 4;
            
            fromBuffer.position(fromIntOffset);
            fromBuffer.get(transfer, 0, fromIntCount);
            
            AllocationManager.claimAllocation(pipeline, fromByteCount, ref ->
            {
                final int byteOffset = ref.byteOffset();
                final int byteCount = ref.byteCount();
                final int intLength = byteCount / 4;
                final IntBuffer intBuffer = ref.intBuffer();

                // no splitting, need 1:1
                assert byteCount == fromByteCount;
                
                intBuffer.position(byteOffset / 4);
                intBuffer.put(transfer, 0, intLength);
                ref.retain(delegate);
                swaps.add(Pair.of(delegate, ref));
            });
        });  
        return swaps;
    }

    /**
     * Called after rebuffered retainers are flushed and swapped with ours.
     */
    void clearRetainers()
    {
        this.retainedBytes.set(0);
        this.retainers.clear();
    }

    public boolean isFinal()
    {
        return this.isFinal;
    }

    public boolean isMapped()
    {
        return this.isMapped;
    }
    
    public boolean isMappedReadOnly()
    {
        return this.isMappedReadonly;
    }
}
