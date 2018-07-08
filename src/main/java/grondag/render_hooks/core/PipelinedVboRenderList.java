package grondag.render_hooks.core;

import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.util.BlockRenderLayer;

public class PipelinedVboRenderList extends VboRenderList
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        super.renderChunkLayer(layer);
    }
}
