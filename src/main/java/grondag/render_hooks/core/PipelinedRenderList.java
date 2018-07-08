package grondag.render_hooks.core;

import net.minecraft.client.renderer.RenderList;
import net.minecraft.util.BlockRenderLayer;

public class PipelinedRenderList extends RenderList
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        super.renderChunkLayer(layer);
    }
}
