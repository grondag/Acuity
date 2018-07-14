package grondag.render_hooks.core;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import grondag.render_hooks.RenderHooks;
import net.minecraft.client.Minecraft;
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
                // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
                // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                GL20.glEnableVertexAttribArray(0);
                GL20.glEnableVertexAttribArray(1);
                
                // we don't want the lightmap at all
                // will be disabled by caller anyway when we return
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                Minecraft.getMinecraft().entityRenderer.disableLightmap();
                
                for (RenderChunk renderchunk : this.renderChunks)
                {
                    CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(layer.ordinal());
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderchunk);
                    renderchunk.multModelviewMatrix();
                    vertexbuffer.renderChunk();
                    GlStateManager.popMatrix();
                }
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                GL20.glDisableVertexAttribArray(0);
                GL20.glDisableVertexAttribArray(1);
                GL20.glDisableVertexAttribArray(2);
                GL20.glDisableVertexAttribArray(3);
                GL20.glDisableVertexAttribArray(4);
                GL20.glDisableVertexAttribArray(5);
                
                OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
                OpenGlHelper.glUseProgram(0);
                GlStateManager.resetColor();
                this.renderChunks.clear();
            }
        }
        else
            super.renderChunkLayer(layer);
    }
}
