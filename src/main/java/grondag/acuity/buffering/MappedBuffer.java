package grondag.acuity.buffering;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL15;

import grondag.acuity.opengl.GLBufferStore;
import grondag.acuity.opengl.OpenGlHelperExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

public class MappedBuffer
{
    public final int glBufferId;
    private @Nullable ByteBuffer mapped = null;
    private boolean isMapped = false;
    private final ConcurrentLinkedQueue<BufferAllocation> flushes = new ConcurrentLinkedQueue<>();
    
    @Nullable BufferAllocation root;
    
    MappedBuffer()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        this.glBufferId = OpenGlHelper.glGenBuffers();
        bind();
        OpenGlHelperExt.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, BufferSlice.MAX_BUFFER_BYTES, GL15.GL_DYNAMIC_DRAW);
        OpenGlHelperExt.handleAppleMappedBuffer();
        map();
        unbind();
    }
    
    public ByteBuffer byteBuffer()
    {
        assert mapped != null;
        return mapped;
    }
    
    private void map()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        mapped = OpenGlHelperExt.mapBufferAsynch(mapped, BufferSlice.MAX_BUFFER_BYTES);
        isMapped = true;
    }
    
    
    /** Called for buffers that are being flushed or reused.*/
    public void remap()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        bind();
        map();
        unbind();
    }
    
    void bind()
    {
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
    }
    
    private void unbind()
    {
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }
    
    /**
     * Called each tick to send updates to GPU. Synchronized to prevent any content upload in between.
     */
    public void flush()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        
        BufferAllocation ref = flushes.poll();
        
        if(ref == null)
            return;
        
        assert isMapped;
        
        bind();
        while(ref != null)
        {
            System.out.println("flush " + this.toString() + " " + ref.byteOffset + " " + ref.byteCount());
            OpenGlHelperExt.flushBuffer(ref.byteOffset, ref.byteCount());
            ref = flushes.poll();
        }
        
        //TODO: need locking here to prevent buffer access while unmapped?
        OpenGlHelperExt.unmapBuffer();
        remap();
        unbind();
    }

    /**
     * Causes part of buffer to be flushed next time we flush.
     */
    public void flushLater(BufferAllocation delegate)
    {
        flushes.add(delegate);
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
}
