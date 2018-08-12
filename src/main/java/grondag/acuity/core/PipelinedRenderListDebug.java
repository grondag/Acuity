package grondag.acuity.core;

import grondag.acuity.Acuity;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelinedRenderListDebug extends AbstractPipelinedRenderList
{
    protected long startNanos;
    protected long totalNanos;
    protected int frameCounter;
    protected int chunkCounter;
    protected int drawCounter;
    protected int quadCounter;
    
    @Override
    public final void addRenderChunk(RenderChunk renderChunkIn, BlockRenderLayer layer)
    {
        chunkCounter++;
        CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderChunkIn.getVertexBufferByLayer(layer.ordinal());
        drawCounter += vertexbuffer.drawCount();
        quadCounter += vertexbuffer.quadCount();
        super.addRenderChunk(renderChunkIn, layer);
    }

    @Override
    public final void renderChunkLayer(BlockRenderLayer layer)
    {
        startNanos = System.nanoTime();
        // assumes will always be a solid layer - probably true enough for us
        if(layer == BlockRenderLayer.SOLID)
            frameCounter++;
        
        super.renderChunkLayer(layer);
        
        totalNanos += (System.nanoTime() - startNanos);
        if(frameCounter >= 600)
        {
            final double ms = totalNanos / 1000000.0;
            String msg = this.isAcuityEnabled ? "ENABLED" : "Disabled";
            Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %d frames / %d chunks / %d draws / %d quads (Acuity API %s)", frameCounter, chunkCounter, drawCounter, quadCounter, msg));
            Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %f ms / %f ms / %f ms / %f ns", ms / frameCounter, ms / chunkCounter, ms / drawCounter, (double)totalNanos / quadCounter));
            totalNanos = 0;
            frameCounter = 0;
            chunkCounter = 0;
            drawCounter = 0;
            quadCounter = 0;
        }
    }
}
