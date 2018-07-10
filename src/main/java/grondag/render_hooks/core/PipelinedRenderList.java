package grondag.render_hooks.core;

import grondag.render_hooks.RenderHooks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelinedRenderList extends RenderList
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
                    CompiledChunk cc = renderchunk.getCompiledChunk();
                    if(cc.isLayerStarted(layer))
                    {
                        GlStateManager.pushMatrix();
                        this.preRenderChunk(renderchunk);
                        GlStateManager.callList(((ListedRenderChunk)renderchunk).getDisplayList(layer, cc));
                        GlStateManager.popMatrix();
                    }
                }
    
                GlStateManager.resetColor();
                this.renderChunks.clear();
            }
        }
        else 
            super.renderChunkLayer(layer);
    }
}