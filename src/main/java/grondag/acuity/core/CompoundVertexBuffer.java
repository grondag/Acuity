package grondag.acuity.core;

import javax.annotation.Nullable;

import grondag.acuity.Acuity;
import grondag.acuity.core.BufferStore.ExpandableByteBuffer;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Multi-pipeline version of VertexBuffer.<p>
 * 
 * RenderChunk keeps a separate VertexBuffer for each BlockRenderLayer,
 * so we can assume that all pipeline IDs are for a single layer.
 */
@SideOnly(Side.CLIENT)
public class CompoundVertexBuffer extends VertexBuffer
{
    private VertexBufferInner inner = VertexBufferInner.claim();
    private @Nullable VertexBufferInner nextInner;
    
    private void checkInner()
    {
        VertexBufferInner next = nextInner;
        if(next != null && next.isReady())
        {
            VertexBufferInner swap = inner;
            inner = next;
            VertexBufferInner.release(swap);
            nextInner = null;
        }
    }
    
    public int drawCount()
    {
        return Acuity.isModEnabled() ?  this.inner.packingList().size() : 1;
    }
    
    public int quadCount()
    {
        return Acuity.isModEnabled() ? this.inner.packingList().quadCount() : this.count / 4;
    }
    
    public CompoundVertexBuffer(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);
    }

//    private static int maxSize = 0;
    
    public final void upload(ExpandableByteBuffer uploadBuffer, VertexPackingList packing)
    {
        checkInner();
        VertexBufferInner next = this.nextInner;
        if(next == null)
        {
            next = VertexBufferInner.claim();
            nextInner = next;
        }
        next.upload(uploadBuffer, packing);
    }
    
    @SuppressWarnings("null")
    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
        if(this.inner != null)
        {
            VertexBufferInner.release(inner);
            this.inner = null;
        }
        if(this.nextInner != null)
        {
            VertexBufferInner.release(nextInner);
            this.nextInner = null;
        }
    }

    /**
     * Renders all uploaded vbos.
     */
    public final void renderChunkTranslucent()
    {
        checkInner();
        inner.renderChunkTranslucent();
    }
    
    public final void prepareSolidRender()
    {
        checkInner();
        inner.prepareSolidRender();
    }
    
    public VertexPackingList packingList()
    {
        checkInner();
        return inner.packingList();
    }
    
    public final void renderSolidNext()
    {
        // note no call to checkInner() cuz render is stateful, don't want to switch after prep
        inner.renderSolidNext();
    }
}
