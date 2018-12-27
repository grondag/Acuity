package grondag.acuity.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.RenderChunk;

@Environment(EnvType.CLIENT)
public class PipelinedRenderList extends AbstractPipelinedRenderList
{
    public PipelinedRenderList()
    {
        super();
    }

    @Override
    public final void addRenderChunk(RenderChunk renderChunkIn, BlockRenderLayer layer)
    {
        super.addRenderChunk(renderChunkIn, layer);
    }

    @Override
    public final void renderChunkLayer(BlockRenderLayer layer)
    {
        super.renderChunkLayer(layer);
    }
}
