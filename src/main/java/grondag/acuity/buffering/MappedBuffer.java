package grondag.acuity.buffering;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import grondag.acuity.Configurator;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.core.PipelineVertexFormat;
import grondag.acuity.opengl.GLBufferStore;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.opengl.VaoStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class MappedBuffer
{
    /**
     * VAO Buffer name if enabled and initialized.
     */
    private int vaoBufferId = -1;
    private boolean vaoNeedsRefresh = true;
    private PipelineVertexFormat format = Configurator.lightingModel.vertexFormat(TextureFormat.SINGLE);
    
    public final int glBufferId;
    private @Nullable ByteBuffer mapped = null;
    private boolean isMapped = false;
    private final ConcurrentLinkedQueue<IBufferAllocation> flushes = new ConcurrentLinkedQueue<>();
    
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
    
    public void setFormat(TextureFormat textureFormat)
    {
        this.format = Configurator.lightingModel.vertexFormat(textureFormat);
        this.vaoNeedsRefresh = true;
    }
    
    /** Called for buffers that are being flushed or reused.*/
    public void remap()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        bind();
        map();
        unbind();
    }

    private void bindVertexAttributesInner()
    {
        OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), format.stride, 0);
        format.bindAttributeLocations(0);
    }
    
    void bind()
    {
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
    }
    
    public void bindVertexAttributes()
    {
        if(vaoNeedsRefresh)
        {
            if(OpenGlHelperExt.isVaoEnabled())
            {
                if(vaoBufferId != -1)
                    vaoBufferId = VaoStore.claimVertexArray();
            }
            OpenGlHelperExt.glBindVertexArray(vaoBufferId);
            GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            OpenGlHelperExt.enableAttributesVao(format.attributeCount);
            bindVertexAttributesInner();
            vaoNeedsRefresh = false;
        }
        
        if(vaoBufferId > -1)
            OpenGlHelperExt.glBindVertexArray(vaoBufferId);
        else        
            // no VAO and must rebind each time
            bindVertexAttributesInner();
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
        
        IBufferAllocation ref = flushes.poll();
        
        if(ref == null)
            return;
        
        assert isMapped;
        
        bind();
        while(ref != null)
        {
            OpenGlHelperExt.flushBuffer(ref.byteOffset(), ref.byteCount());
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
    public void flushLater(IBufferAllocation delegate)
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
            
            if(this.vaoBufferId > 0)
            {
                VaoStore.releaseVertexArray(vaoBufferId);
                vaoBufferId = -1;
            }
        }
    }
    
    private boolean isDisposed = false;
    
    public boolean isDisposed()
    {
        return isDisposed;
    }
}
