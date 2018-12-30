package grondag.acuity.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderer;

@Environment(EnvType.CLIENT)
public class PipelinedRenderList extends AbstractPipelinedRenderList
{
    public PipelinedRenderList()
    {
        super();
    }

    @Override
    public final void add(ChunkRenderer renderChunkIn, BlockRenderLayer layer)
    {
        super.add(renderChunkIn, layer);
    }

    @Override
    public final void renderChunkLayer(BlockRenderLayer layer)
    {
        super.renderChunkLayer(layer);
    }
}
