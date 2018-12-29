package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderWorker;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker
{
    @Redirect(method = "processTask", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;isLayerStarted(Lnet/minecraft/util/BlockRenderLayer;)Z"))
    private boolean isLayerStarted(ChunkRenderData compiledChunk, BlockRenderLayer layer)
    {
        return PipelineHooks.shouldUploadLayer(compiledChunk, layer);
    }
}
