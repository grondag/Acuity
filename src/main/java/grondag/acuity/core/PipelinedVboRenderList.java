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
    private int quadCount;
    private long start;
    
    
//    protected final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if(Acuity.DEBUG)
        {
            
            if (!this.renderChunks.isEmpty() && this.initialized)
            {
                chunkCount += this.renderChunks.size();
                for (RenderChunk renderchunk : this.renderChunks)
                {
                    CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(layer.ordinal());
                    drawCount += vertexbuffer.drawCount();
                    quadCount += vertexbuffer.quadCount();
                }
            }
            start = System.nanoTime();
        }
        
        if(Acuity.isModEnabled())
        {
            // FAIL: unfortunately using explicit uniforms is slower
            // Forge doesn't give us a hook in the render loop that comes
            // after camera transform is set up - so call out event handler
            // here as a workaround. Our event handler will only act 1x/frame.
//            PipelineManager.INSTANCE.beforeRenderChunks();
            
            if (!this.renderChunks.isEmpty() && this.initialized)
            {
             // FAIL: unfortunately using explicit uniforms is slower
//                modelViewMatrixBuffer.position(0);
//                GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrixBuffer);
//                Matrix4f mvMatrix = new Matrix4f();
//                mvMatrix.loadTranspose(modelViewMatrixBuffer);
//                
//                Matrix4f xlatMatrix = new Matrix4f();
//                xlatMatrix.setIdentity();
                
                // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
                // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.
                
                for (RenderChunk renderchunk : this.renderChunks)
                {
                    CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(layer.ordinal());
                    
                 // FAIL: unfortunately using explicit uniforms is slower
//                    BlockPos blockpos = renderchunk.getPosition();
//                    // note row-major order in the matrix library we are using
//                    xlatMatrix.m03 = (float)((double)blockpos.getX() - this.viewEntityX);
//                    xlatMatrix.m13 = (float)((double)blockpos.getY() - this.viewEntityY);
//                    xlatMatrix.m23 = (float)((double)blockpos.getZ() - this.viewEntityZ);
//                    
//                    Matrix4f mvPos = Matrix4f.mul(xlatMatrix, mvMatrix, null);
//                    Matrix4f mvChunk = new Matrix4f();
//                    mvChunk.loadTranspose(renderchunk.modelviewMatrix);
//                    // RenderChunk.multModelviewMatrix() may crash due to buffer overrun without this
//                    renderchunk.modelviewMatrix.position(0);
//                    Matrix4f mvm = Matrix4f.mul(mvChunk, mvPos, null);
                    
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderchunk);
                    renderchunk.multModelviewMatrix();
 
                    vertexbuffer.renderChunk();
//                    vertexbuffer.renderChunk(mvm);
                    GlStateManager.popMatrix();
                }
                
                if(OpenGlHelperExt.isVaoEnabled())
                    OpenGlHelperExt.glBindVertexArray(0);
                
                OpenGlHelperExt.enableAttributes(0);
                OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
                OpenGlHelperExt.glUseProgramFast(0);
                GlStateManager.resetColor();
                this.renderChunks.clear();
            }
        }
        else
        {
            super.renderChunkLayer(layer);
        }
        
        if(Acuity.DEBUG)
        {
            totalNanos += (System.nanoTime() - start);
            if(++runCount >= 2000)
            {
                double ms = totalNanos / 1000000.0;
                String msg = Acuity.isModEnabled() ? "ENABLED" : "Disabled";
                Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %d calls / %d chunks / %d draws / %d quads (Acuity API %s)", runCount, chunkCount, drawCount, quadCount, msg));
                Acuity.INSTANCE.getLog().info(String.format("renderChunkLayer %f ms / %f ms / %f ms / %f ns", ms / runCount, ms / chunkCount, ms / drawCount, (double)totalNanos / quadCount));
                totalNanos = 0;
                runCount = 0;
                chunkCount = 0;
                drawCount = 0;
                quadCount = 0;
            }
        }
    }
}
