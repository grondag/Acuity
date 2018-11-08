package grondag.acuity.buffering;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL15;

import grondag.acuity.Acuity;
import grondag.acuity.opengl.GLBufferStore;
import grondag.acuity.opengl.OpenGlHelperExt;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

public class MappedBuffer
{
    public static final int CAPACITY_BYTES = 0x80000;
    
    public static final int UNABLE_TO_ALLOCATE = -1;
    
    //TODO: disable
    public static final ObjectArrayList<MappedBuffer> inUse = new ObjectArrayList<>();
    public int maxRetainCount = 0;
    public int maxBytes = 0;
    public int currentBytes = 0;
    
    public final int glBufferId;
    private int currentByteOffset = 0;
    private int currentDirtyOffset = 0;
    private @Nullable ByteBuffer mapped = null;
    private boolean isMapped = false;
    private boolean isFinal = false;
    
    //PERF: switch back to a counter
    private final Object2IntMap<Object> retainers = Object2IntMaps.synchronize(new Object2IntOpenHashMap<Object>());
    
    MappedBuffer()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        
        // prevents 0-byte store retainer from being discarded
        retainers.defaultReturnValue(-1);
        
        this.glBufferId = OpenGlHelper.glGenBuffers();
        bind();
        orphan();
        map();
        unbind();
        
        
        //PERF: is always on client thread - why synchronize?
        synchronized(inUse)
        {
            inUse.add(this);
        }
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
     * Will return null if can't fit.
     * Used for translucent render.
     */
    public synchronized @Nullable IMappedBufferReference requestBytes(int byteCount)
    {
        assert mapped != null;
        
        final int oldOffset = currentByteOffset;
        final int newOffset = oldOffset + byteCount;
        if(newOffset > CAPACITY_BYTES)
            return null;
        
        currentByteOffset = newOffset;
        return new SimpleMappedBufferReference(this, oldOffset, byteCount);
    }
    
    /**
     * Won't break stride.
     * Used for solid renders that all share same pipeline/vertex format.
     */
    public synchronized @Nullable IMappedBufferReference requestBytes(int byteCount, int stride)
    {
        assert mapped != null;
        
        int filled = ((CAPACITY_BYTES - currentByteOffset) / stride) * stride;
        
        if(filled <= 0)
            return null;
        
        if(filled > byteCount)
            filled = byteCount;
        
        SimpleMappedBufferReference result =  new SimpleMappedBufferReference(this, currentByteOffset, filled);
        currentByteOffset += filled;
        return result;
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
     * Called each tick to send updates to GPU. Synchronized to prevent any content upload in between.
     */
    public synchronized void flush()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        
        if(currentDirtyOffset == currentByteOffset && !isMapped)
            return;
        
        bind();
        if(currentDirtyOffset != currentByteOffset)
        {
            assert isMapped;
            OpenGlHelperExt.flushBuffer(currentDirtyOffset, currentByteOffset - currentDirtyOffset);
            currentDirtyOffset = currentByteOffset;
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
    public synchronized void retain(Object retainer, int bytes)
    {
        retainers.put(retainer, bytes);
        // don't count store retainer
        if(bytes != 0)
        {
            maxRetainCount++;
            maxBytes += bytes;
            currentBytes += bytes;
        }
    }
    
    public synchronized void release(Object retainer)
    {
        final int bytes = retainers.removeInt(retainer);
        currentBytes -= bytes;
        
        if(retainer == MappedBufferStore.STORE_RETAINER)
            this.isFinal = true;
        else
            assert bytes != 0;
        
        if(retainers.isEmpty())
            MappedBufferStore.scheduleRelease(this);
    }
    
    public void reportStats()
    {
        Acuity.INSTANCE.getLog().info(String.format("Buffer %d: MaxCount=%d  MaxBytes=%d    Count=%d  Bytes=%d   (%d / %d)",
                this.glBufferId, this.maxRetainCount, this.maxBytes, this.retainers.size(), this.currentBytes,
                this.maxRetainCount == 0 ? 0 : this.retainers.size() * 100 / this.maxRetainCount,
                this.maxBytes == 0 ? 0 : this.currentBytes * 100 / this.maxBytes));
        
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
        currentByteOffset = 0;
        currentDirtyOffset = 0;
        maxRetainCount = 0;
        maxBytes = 0;
        currentBytes = 0;
    }
}
