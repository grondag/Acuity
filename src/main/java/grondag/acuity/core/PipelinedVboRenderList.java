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
//    private long totalNanos;
//    private int runCount;
    
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
//        long start = System.nanoTime();
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
            super.renderChunkLayer(layer);
        
//        totalNanos += (System.nanoTime() - start);
//        if(++runCount >= 600)
//        {
//            Acuity.INSTANCE.getLog().info("Milliseconds per renderChunkLayer: " + totalNanos / runCount / 1000000f);
//            totalNanos = 0;
//            runCount = 0;
//        }
    }
}