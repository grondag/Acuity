package grondag.acuity.buffering;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

public class MappedBuffer
{
    public static final int CAPACITY_BYTES = 0x80000;
    private static final int HALF_CAPACITY = CAPACITY_BYTES / 2;
    
    public static final ObjectArrayList<MappedBuffer> inUse = new ObjectArrayList<>();
    
    public final int glBufferId;
    private final AtomicInteger currentMaxOffset = new AtomicInteger();
    private int lastFlushedOffset = 0;
    private @Nullable ByteBuffer mapped = null;
    private boolean isMapped = false;
    private boolean isFinal = false;
    
    private final AtomicInteger retainedBytes = new AtomicInteger();
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
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        mapped = OpenGlHelperExt.mapBufferAsynch(mapped, CAPACITY_BYTES, writeFlag);
        isMapped = true;
    }
    
    /** Called for buffers that are being reused.  Should already have been orphaned earlier.*/
    public void remap()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        bind();
        map(true);
        unbind();
    }
    
    /**
     * Won't break stride.
     */
    public @Nullable IMappedBufferDelegate requestBytes(int byteCount, int stride)
    {
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
    
    /**
     * Called each tick to send updates to GPU. 
     * TODO: locking to prevent content upload during flush.
     */
    public void flush()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        
        final int currentMax = currentMaxOffset.get();
        final int bytes = currentMax - lastFlushedOffset;
        
        if(bytes == 0 && !isMapped)
            return;
        
        bind();
        if(bytes != 0)
        {
            assert isMapped;
            OpenGlHelperExt.flushBuffer(lastFlushedOffset, bytes);
            lastFlushedOffset = currentMax;
        }
        
        if(isMapped)
        {
            OpenGlHelperExt.unmapBuffer();
            if(isFinal)
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
        retainers.remove(drawable);
        final int bytes = drawable.bufferDelegate().byteCount();
        final int newRetained = retainedBytes.addAndGet(-bytes);
        if(newRetained < HALF_CAPACITY && (newRetained + bytes) >= HALF_CAPACITY)
        {
            assert isFinal;
            assert !isMapped;
            MappedBufferStore.scheduleRelease(this);
        }
    }
    
    public void reportStats()
    {
        Acuity.INSTANCE.getLog().info(String.format("Buffer %d: Count=%d  Bytes=%d (%d)",
                this.glBufferId, this.retainers.size(), this.retainedBytes.get(),
                this.retainedBytes.get() * 100 / CAPACITY_BYTES));
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
        }
        orphan();
        isFinal = false;
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
        assert isMapped;
        
        @SuppressWarnings("null")
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
            
            MappedBufferStore.claimAllocation(pipeline, fromByteCount, ref ->
            {
                final int byteOffset = ref.byteOffset();
                final int byteCount = ref.byteCount();
                final int intLength = byteCount / 4;
                final IntBuffer intBuffer = ref.intBuffer();

                // no splitting, need 1:1
                assert byteCount == fromByteCount;
                
                intBuffer.position(byteOffset / 4);
                intBuffer.put(transfer, 0, intLength);
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
}
