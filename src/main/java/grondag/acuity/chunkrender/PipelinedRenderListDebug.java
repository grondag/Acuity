/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.chunkrender;

import grondag.acuity.Acuity;
import grondag.acuity.buffer.DrawableChunk;
import grondag.acuity.mixin.extension.ChunkRendererExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderer;

@Environment(EnvType.CLIENT)
public class PipelinedRenderListDebug extends AbstractPipelinedRenderList
{
    public PipelinedRenderListDebug()
    {
        super();
    }

    protected long startNanos;
    protected long totalNanos;
    protected int frameCounter;
    protected int chunkCounter;
    protected int drawCounter;
    protected int quadCounter;
    protected long minNanos = Long.MAX_VALUE;
    protected long maxNanos = 0;
    
    @Override
    public final void add(ChunkRenderer renderChunkIn, BlockRenderLayer layer)
    {
        chunkCounter++;
        DrawableChunk vertexbuffer = layer == BlockRenderLayer.SOLID
                ? ((ChunkRendererExt)renderChunkIn).getSolidDrawable()
                : ((ChunkRendererExt)renderChunkIn).getTranslucentDrawable();
        if(vertexbuffer == null)
            return;
        drawCounter += vertexbuffer.drawCount();
        quadCounter += vertexbuffer.quadCount();
        super.add(renderChunkIn, layer);
    }

    @Override
    public final void renderChunkLayer(BlockRenderLayer layer)
    {
        startNanos = System.nanoTime();
        // assumes will always be a solid layer - probably true enough for us
        if(layer == BlockRenderLayer.SOLID)
            frameCounter++;
        
        super.renderChunkLayer(layer);
        
        long duration = (System.nanoTime() - startNanos);
        minNanos = Math.min(minNanos, duration);
        maxNanos = Math.max(maxNanos, duration);
        totalNanos += duration;
        if(frameCounter >= 600)
        {
            final double ms = totalNanos / 1000000.0;
            String msg = this.isAcuityEnabled ? "ENABLED" : "Disabled";
            Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %d frames / %d chunks / %d draws / %d quads (Acuity API %s)", frameCounter, chunkCounter, drawCounter, quadCounter, msg));
            Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %f ms / %f ms / %f ms / %f ns", ms / frameCounter, ms / chunkCounter, ms / drawCounter, (double)totalNanos / quadCounter));
            Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer min = %f ms, max = %f ms", minNanos / 1000000.0, maxNanos / 1000000.0));
            
            totalNanos = 0;
            frameCounter = 0;
            chunkCounter = 0;
            drawCounter = 0;
            quadCounter = 0;
            minNanos = Long.MAX_VALUE;
            maxNanos = 0;
        }
    }
}
