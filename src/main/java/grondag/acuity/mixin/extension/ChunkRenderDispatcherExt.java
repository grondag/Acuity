package grondag.acuity.mixin.extension;

import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;

public interface ChunkRenderDispatcherExt
{

    ChunkRenderer getChunk(BlockPos blockPos);

}
