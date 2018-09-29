package grondag.acuity.core;

import grondag.acuity.Acuity;
import grondag.acuity.core.BufferStore.ExpandableByteBuffer;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
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
    private ObjectArrayFIFOQueue<VertexBufferInner> nextInner = new ObjectArrayFIFOQueue<VertexBufferInner>();
    
    private void checkInner()
    {
        while(tryAdvance()) {}
    }
    
    private boolean tryAdvance()
    {
        if(nextInner.isEmpty()) return false;
        
        VertexBufferInner next = nextInner.first();
        if(next.isReady())
        {
            nextInner.dequeue();
            VertexBufferInner swap = inner;
            inner = next;
            VertexBufferInner.release(swap);
            return true;
        }
        return false;
    }
    
    public int drawCount()
    {
        return Acuity.isModEnabled() 
                ? (this.inner.isReady() ? this.inner.packingList().size() : 0)
                : 1;
    }
    
    public int quadCount()
    {
        return Acuity.isModEnabled() 
                ? (this.inner.isReady() ? this.inner.packingList().quadCount() : 0)
                : this.count / 4;
    }
    
    public CompoundVertexBuffer(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);
    }

//    private static int maxSize = 0;
    
    public final void upload(ExpandableByteBuffer uploadBuffer, VertexPackingList packing)
    {
        VertexBufferInner next = VertexBufferInner.claim();
        next.upload(uploadBuffer, packing);
        nextInner.enqueue(next);
    }
    
    @SuppressWarnings("null")
    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
        if(this.inner != null)
        {
            this.inner.deleteGlBuffers();
        }
        while(!nextInner.isEmpty())
        {
            nextInner.dequeue().deleteGlBuffers();
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
        return inner.isReady() ? inner.packingList() : DummyPackingList.INSTANCE;
    }
    
    public final void renderSolidNext()
    {
        // note no call to checkInner() cuz render is stateful, don't want to switch after prep
        inner.renderSolidNext();
    }
}
