package grondag.acuity.mixin.extension;

import net.minecraft.client.render.block.BlockRenderLayer;

public interface ChunkRenderDataExt
{
    void clear();

    void setNonEmpty(BlockRenderLayer blockRenderLayer);
    
    Object getVisibilityData();
}
