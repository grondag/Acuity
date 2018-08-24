package grondag.acuity.core;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import grondag.acuity.Acuity;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Redirects VertexBuffer methods when Acuity is enabled.<p>
 */
@SideOnly(Side.CLIENT)
public class CompoundVertexBuffer extends VertexBuffer
{
    private AbstractVertexPackingRenderer currentRenderer = EmptyVertexPackingRenderer.INSTANCE;
    private @Nullable AbstractVertexPackingRenderer nextRenderer = null;
    private int frameWaits = 0;
    
    public int drawCount()
    {
        return Acuity.isModEnabled() ?  this.currentRenderer.size() : 1;
    }
    
    public int quadCount()
    {
        return Acuity.isModEnabled() ? this.currentRenderer.quadCount() : this.count / 4;
    }
    
    
    public CompoundVertexBuffer(VertexFormat     vertexFormatIn)
    {
        super(vertexFormatIn);
    }

    public final void upload(ByteBuffer buffer, VertexPackingList packing)
    {
        AbstractVertexPackingRenderer next = RendererStore.claim();
        next.upload(buffer, packing);
        this.nextRenderer = next;
        this.frameWaits = 1;
    }

    /**
     * Renders all uploaded vbos.
     */
    public final void renderChunk(boolean isSolidLayer)
    {
        AbstractVertexPackingRenderer renderer = this.nextRenderer;
        if(renderer == null)
        {
            renderer = this.currentRenderer;
        }
        else
        {
//            if(renderer.isReady())
            if(this.frameWaits-- == 0)
            {
                releaseRenderer(this.currentRenderer);
                this.currentRenderer = renderer;
                this.nextRenderer = null;
            }
            else
                renderer = this.currentRenderer;
        }
        
        if(renderer.size() == 0)
            return;
        
        renderer.render(isSolidLayer);
    }

    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
        releaseRenderer(this.currentRenderer);
        releaseRenderer(this.nextRenderer);
    }
    
    private void releaseRenderer(@Nullable AbstractVertexPackingRenderer renderer)
    {
        if(renderer == null || renderer == EmptyVertexPackingRenderer.INSTANCE)
            return;
        RendererStore.release(renderer);
    }
    
    
}
