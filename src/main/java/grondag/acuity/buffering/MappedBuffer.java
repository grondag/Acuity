package grondag.acuity.buffering;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.APPLEFlushBufferRange;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import grondag.acuity.opengl.OpenGlHelperExt;
import net.minecraft.client.renderer.OpenGlHelper;

public class MappedBuffer
{
    private static final int CAPACITY_BYTES = 0x200000;
    
    public static final int UNABLE_TO_ALLOCATE = -1;
            
    public final int glBufferId;
    private int currentByteOffset = 0;
    private int currentDirtyOffset = 0;
    private int retainCount = 0;
    private @Nullable ByteBuffer mapped = null;
    
    MappedBuffer()
    {
        this.glBufferId = OpenGlHelper.glGenBuffers();
        bind();
        orphan();
        map();
        unbind();
    }
    
    @SuppressWarnings("null")
    public ByteBuffer byteBuffer()
    {
        return mapped;
    }
    
    private void map()
    {
        mapped = OpenGlHelperExt.mapBuffer(mapped, CAPACITY_BYTES);
    }
    
    /** Called for buffers that are being reused.  Should already have been orphaned earlier.*/
    public void remap()
    {
        bind();
        map();
        unbind();
    }

    /**
     * Will return {@link #UNABLE_TO_ALLOCATE} if can't fit.
     * Otherwise returns starting offset, which may be zero.
     * Used for translucent render.
     */
    public synchronized int requestBytes(int byteCount)
    {
        final int oldOffset = currentByteOffset;
        final int newOffset = oldOffset + byteCount;
        if(newOffset > CAPACITY_BYTES)
            return UNABLE_TO_ALLOCATE;
        
        retain();
        currentByteOffset = newOffset;
        return oldOffset;
    }
    
    /**
     * Will return offset (low) and allocated bytes (high) as packed ints. 
     * Won't break stride.
     * Used for solid renders that all share same pipeline/vertex format.
     */
    public synchronized long requestBytes(int byteCount, int stride)
    {
        int filled = ((CAPACITY_BYTES - currentByteOffset) / stride) * stride;
        
        if(filled <= 0)
            return 0;
        
        if(filled > byteCount)
            filled = byteCount;
        
        retain();
        long result =  (((long)filled) << 32) | currentByteOffset;
        currentByteOffset += filled;
        return result;
    }

    private void bind()
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
        OpenGlHelperExt.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, CAPACITY_BYTES, GL15.GL_STATIC_DRAW);
        OpenGlHelperExt.handleAppleMappedBuffer();
    }
    
    /**
     * Called each tick to send updates to GPU. Synchronized to prevent any content upload in between.
     */
    public synchronized void flush()
    {
        if(currentDirtyOffset != currentByteOffset)
        {
            bind();
            if(OpenGlHelperExt.appleMapping)
            {
                APPLEFlushBufferRange.glFlushMappedBufferRangeAPPLE(OpenGlHelper.GL_ARRAY_BUFFER, currentDirtyOffset, currentByteOffset - currentDirtyOffset);
            }
            else
            {
                GL30.glFlushMappedBufferRange(OpenGlHelper.GL_ARRAY_BUFFER, currentDirtyOffset, currentByteOffset - currentDirtyOffset);
            }
            GL15.glUnmapBuffer(OpenGlHelper.GL_ARRAY_BUFFER);
            map();
            currentDirtyOffset = currentByteOffset;
            unbind();
        }
    }

    /**
     * Called implicitly when bytes are allocated.
     * Store calls explicitly to retain while this buffer is being filled.
     */
    public synchronized void retain()
    {
        retainCount++;
    }
    
    public synchronized void release()
    {
        retainCount--;
        if(retainCount == 0)
        {
            currentByteOffset = 0;
            currentDirtyOffset = 0;
            retainCount = 0;
            mapped = null;
            bind();
            orphan();
            unbind();
            MappedBufferStore.release(this);
        }
    }
}
