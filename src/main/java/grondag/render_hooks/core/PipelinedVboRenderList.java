package grondag.render_hooks.core;

import grondag.render_hooks.RenderHooks;
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
    private final boolean isModEnabled = RenderHooks.isModEnabled();
    
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if(isModEnabled)
        {
            if (!this.renderChunks.isEmpty() && this.initialized)
            {
                for (RenderChunk renderchunk : this.renderChunks)
                {
                    CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(layer.ordinal());
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderchunk);
                    renderchunk.multModelviewMatrix();
                    vertexbuffer.renderChunk();
                    GlStateManager.popMatrix();
                }
    
                OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
                GlStateManager.resetColor();
                this.renderChunks.clear();
            }
        }
        else
            super.renderChunkLayer(layer);
    }
}
