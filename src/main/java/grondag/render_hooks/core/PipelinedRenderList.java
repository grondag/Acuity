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
    private long totalNanos;
    private int runCount;
    
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        long start = System.nanoTime();
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
        
        totalNanos += (System.nanoTime() - start);
        if(++runCount >= 6000)
        {
            RenderHooks.INSTANCE.getLog().info("Nanoseconds per renderChunkLayer: " + totalNanos / runCount);
            totalNanos = 0;
            runCount = 0;
        }
    }
}
