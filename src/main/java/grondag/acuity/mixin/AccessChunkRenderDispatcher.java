package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;

//TODO: remove if not used

@Mixin(ChunkRenderDispatcher.class)
public interface AccessChunkRenderDispatcher
{
    @Accessor public ChunkRenderer getChunk(BlockPos blockPos);
}
