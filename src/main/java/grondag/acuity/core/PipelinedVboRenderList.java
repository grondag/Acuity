package grondag.acuity.core;

import grondag.acuity.Acuity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelinedVboRenderList extends VboRenderList
{
    private long totalNanos;
    private int runCount;
    private int chunkCount;
    private int drawCount;
    
    private long start;
    
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if(Acuity.DEBUG)
        {
            start = System.nanoTime();
            chunkCount += this.renderChunks.size();
        }
        
        if(Acuity.isModEnabled())
        {
            
            if (!this.renderChunks.isEmpty() && this.initialized)
            {
                // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
                // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.
                
                for (RenderChunk renderchunk : this.renderChunks)
                {
                    CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(layer.ordinal());
                    
                    if(Acuity.DEBUG)
                        drawCount += vertexbuffer.drawCount();
                    
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderchunk);
                    renderchunk.multModelviewMatrix();
                    vertexbuffer.renderChunk();
                    GlStateManager.popMatrix();
                }
                
                OpenGlHelperExt.enableAttributes(0);
                OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
                OpenGlHelper.glUseProgram(0);
                GlStateManager.resetColor();
                this.renderChunks.clear();
            }
        }
        else
        {
            if(Acuity.DEBUG)
                drawCount += this.renderChunks.size();
            
            super.renderChunkLayer(layer);
        }
        
        if(Acuity.DEBUG)
        {
            totalNanos += (System.nanoTime() - start);
            if(++runCount >= 2000)
            {
                double ms = totalNanos / 1000000.0;
                String msg = Acuity.isModEnabled() ? "ENABLED" : "Disabled";
                Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %d calls / %d chunks / %d draws (Acuity API %s)", runCount, chunkCount, drawCount, msg));
                Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %f / %f / %f ms each", ms / runCount, ms / chunkCount, ms / drawCount));
                totalNanos = 0;
                runCount = 0;
                chunkCount = 0;
                drawCount = 0;
            }
        }
    }
}
