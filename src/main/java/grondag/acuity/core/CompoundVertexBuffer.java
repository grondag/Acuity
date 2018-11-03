package grondag.acuity.core;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import grondag.acuity.Acuity;
import grondag.acuity.buffering.IDrawableBufferDelegate;
import grondag.acuity.buffering.IDrawableChunk;
import grondag.acuity.buffering.IUploadableChunk;
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
public class CompoundVertexBuffer extends VertexBuffer implements IDrawableChunk.Solid, IDrawableChunk.Translucent
{
    private VertexBufferInner inner = VertexBufferInner.claim();
    
    // UGLY: keeping a queue of inner buffers and using fences is overwrought when using new glBuffers each time
    // theoretically, glBufferData should have no dependency on our byte buffer after it returns, but doing it this
    // way eliminated remaining rendering artifacts likely due to concurrency and will probably need the fences later
    // on when using memory-mapped buffers so leaving it for now.  
    private ObjectArrayFIFOQueue<VertexBufferInner> nextInner = new ObjectArrayFIFOQueue<VertexBufferInner>();
    
    @Override
    public void clear()
    {
        VertexBufferInner.release(inner);
        inner = VertexBufferInner.claim();
        while(!nextInner.isEmpty())
            VertexBufferInner.release(nextInner.dequeue());
    }
    
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
    
    @Override
    public int drawCount()
    {
        return Acuity.isModEnabled() 
                ? (this.inner.isReady() ? this.inner.packingList().size() : 0)
                : 1;
    }
    
    @Override
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
    
    @Override
    public final void upload(IUploadableChunk uploadBuffer)
    {
        VertexBufferInner next = VertexBufferInner.claim();
        next.upload(uploadBuffer);
        nextInner.enqueue(next);
    }
    
    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
        this.clear();
    }

    /**
     * Renders all uploaded vbos.
     */
    @Override
    public final void renderChunkTranslucent()
    {
        checkInner();
        inner.renderChunkTranslucent();
    }
    
    @Override
    public final void prepareSolidRender(Consumer<IDrawableBufferDelegate> consumer)
    {
        checkInner();
        inner.prepareSolidRender(consumer);
    }

    @Override
    public void bindBuffer()
    {
        assert !Acuity.isModEnabled();
        super.bindBuffer();
    }

    @Override
    public void bufferData(ByteBuffer data)
    {
        assert !Acuity.isModEnabled();
        super.bufferData(data);
    }

    @Override
    public void drawArrays(int mode)
    {
        assert !Acuity.isModEnabled();
        super.drawArrays(mode);
    }

    @Override
    public void unbindBuffer()
    {
        assert !Acuity.isModEnabled();
        super.unbindBuffer();
    }
    
    
}
