package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.mixin.extension.ChunkRenderDispatcherExt;
import net.minecraft.client.render.chunk.ChunkRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRenderDispatcher implements ChunkRenderDispatcherExt
{
    @Override
    @Shadow public abstract ChunkRenderer getChunk(BlockPos blockPos);
}
