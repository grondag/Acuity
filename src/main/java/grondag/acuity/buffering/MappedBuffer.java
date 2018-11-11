package grondag.acuity.buffering;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL15;

import grondag.acuity.Acuity;
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
    private final ConcurrentLinkedDeque<DrawableChunkDelegate> retainers = new ConcurrentLinkedDeque<>();
    
    MappedBuffer()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        this.glBufferId = OpenGlHelper.glGenBuffers();
        bind();
        orphan();
        map();
        unbind();
        inUse.add(this);
    }
    
    public ByteBuffer byteBuffer()
    {
        assert mapped != null;
        return mapped;
    }
    
    private void map()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        mapped = OpenGlHelperExt.mapBufferAsynch(mapped, CAPACITY_BYTES);
        isMapped = true;
    }
    
    /** Called for buffers that are being reused.  Should already have been orphaned earlier.*/
    public void remap()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        bind();
        map();
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
        retainers.offer(drawable);
    }
    
    public void release(DrawableChunkDelegate drawable)
    {
        final int bytes = drawable.bufferDelegate().byteCount();
        final int newRetained = retainedBytes.addAndGet(-bytes);
        if(newRetained < HALF_CAPACITY && (newRetained + bytes) >= HALF_CAPACITY)
            MappedBufferStore.scheduleRelease(this);
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
        unbind();
        isFinal = false;
        currentMaxOffset.set(0);
        lastFlushedOffset = 0;
    }
}
