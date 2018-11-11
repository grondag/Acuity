package grondag.acuity.buffering;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import grondag.acuity.Configurator;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.core.PipelineVertexFormat;
import grondag.acuity.opengl.Fence;
import grondag.acuity.opengl.OpenGlFenceExt;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.opengl.VaoStore;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public abstract class BufferAllocation
{
    /**
     * VAO Buffer name if enabled and initialized.
     */
    protected int vaoBufferId = -1;
    protected boolean vaoNeedsRefresh = true;
    protected PipelineVertexFormat format = Configurator.lightingModel.vertexFormat(TextureFormat.SINGLE);
    
    public final Fence fence = OpenGlFenceExt.create();
    
    protected BufferSlice slice;
    protected final int byteOffset;
    protected int quadCount;
    protected AtomicBoolean isFree = new AtomicBoolean(true);
    private boolean isDeleted = false;
    
    @Nullable BufferAllocation childA;
    @Nullable BufferAllocation childB;
    
    protected BufferAllocation(BufferSlice slice, int byteOffset)
    {
        this.byteOffset = byteOffset;
        this.slice = slice;
        this.format = Configurator.lightingModel.vertexFormat(slice.format);
        this.vaoNeedsRefresh = true;
    }
    
    public abstract MappedBuffer buffer();

    public final int quadCount()
    {
        assert !isDeleted;
        return quadCount;
    }

    public final void setQuadCount(int quadCount)
    {
        assert !isDeleted;
        this.quadCount = quadCount;
    }
    
    public final boolean claim()
    {
        assert !isDeleted;
        return this.isFree.compareAndSet(true, false);
    }
    
    protected final void delete()
    {
        this.isDeleted = true;
        if(this.vaoBufferId > 0)
        {
            VaoStore.releaseVertexArray(vaoBufferId);
            vaoBufferId = -1;
        }
        this.fence.deleteGlResources();
    }
    
    protected final boolean isDeleted()
    {
        return this.isDeleted;
    }
    
    public final BufferSlice slice()
    {
        return this.slice;
    }
    
    public void release()
    {
        assert !this.isDeleted;
        assert !this.isFree.get();
        this.childA = null;
        this.childB = null;
    }
    
    public final int glBufferId()
    {
        return buffer().glBufferId;
    }

    public final IntBuffer intBuffer()
    {
        return buffer().byteBuffer().asIntBuffer();
    }

    public final boolean isDisposed()
    {
        return buffer().isDisposed();
    }
    
    //TODO: need to flush allocation each and then flush buffer?
    public final void flush()
    {
        buffer().flush();
    }
    
    public final TextureFormat format()
    {
        return slice().format;
    }
    
    public final int quadStride()
    {
        return slice().quadStride;
    }
    
    public final boolean isMin()
    {
        return slice().isMin;
    }
    
    public final boolean isMax()
    {
        return slice().isMax;
    }
    
    public final int byteOffset()
    {
        return this.byteOffset;
    }
    
    public final int byteCount()
    {
        return this.quadStride() * this.quadCount();
    }
    
    public final void flushLater()
    {
        this.buffer().flushLater(this);
    }
    
    public void bind()
    {
        buffer().bind();
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
    
    protected void bindVertexAttributesInner()
    {
        OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), format.stride, byteOffset);
        format.bindAttributeLocations(byteOffset);
    }
    
    public static class Root extends BufferAllocation
    {
        protected final MappedBuffer buffer;
        
        protected Root(BufferSlice slice, MappedBuffer buffer)
        {
            super(slice, 0);
            this.buffer = buffer;
            assert slice.isMax;
        }
        
        @Override
        public MappedBuffer buffer()
        {
            return buffer;
        }
        
        @Override
        public final void release()
        {
            super.release();
            this.isFree.set(true);
            MappedBufferStore.acceptFree(this);
        }
    }
    
    public static class Slice extends BufferAllocation
    {
        private final BufferAllocation parent;
        protected @Nullable Slice buddy;
        
        public Slice(BufferSlice slice, int startVertex, BufferAllocation parent)
        {
            super(slice, startVertex);
            this.parent = parent;
            assert parent.slice.divisionLevel == slice.divisionLevel - 1;
        }
        
        @Override
        public MappedBuffer buffer()
        {
            return parent.buffer();
        }
        
        @SuppressWarnings("null")
        @Override
        public final void release()
        {
            super.release();
            if(this.buddy.claim())
            {
                this.buddy.delete();
                this.delete();
                this.parent.release();
            }
            else
            {
                this.isFree.set(true);
                MappedBufferStore.acceptFree(this);
            }
        }
    }
}
