package grondag.acuity.core;

import org.lwjgl.opengl.GL11;

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
                // We are using generic vertex attributes and don't want any of these.
                // Disabling them here does no harm because caller will disable them anyway when we return.
                //
                // Note that GL_VERTEX_ARRAY can NOT be disabled without breaking everything
                // (on my system at least) even though we are not using it directly in the shader. 
                GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
                
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                
                // was doing  in lieu of calling EntityRenderer.disableLightmap()
                // but caused problems and does not seem needed when using shaders
//                GlStateManager.disableTexture2D();
                
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                
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
            if(++runCount >= 600)
            {
                double ms = totalNanos / 1000000.0;
                Acuity.INSTANCE.getLog().info(String.format("PipelinedVboRenderList %d calls / %d chunks / %d draws", runCount, chunkCount, drawCount));
                Acuity.INSTANCE.getLog().info(String.format("PipelinedVboRenderList %f / %f / %f ms each", ms / runCount, ms / chunkCount, ms / drawCount));
                totalNanos = 0;
                runCount = 0;
                chunkCount = 0;
                drawCount = 0;
            }
        }
    }
}
