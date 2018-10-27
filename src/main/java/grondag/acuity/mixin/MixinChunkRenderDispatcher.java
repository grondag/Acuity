package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.vertex.VertexBuffer;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRenderDispatcher
{
    @Redirect(method = "uploadChunk", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;uploadVertexBuffer(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/client/renderer/vertex/VertexBuffer;)V"))
    private void onUploadVertexBuffer(ChunkRenderDispatcher dispatch, BufferBuilder source, VertexBuffer target)
    {
        PipelineHooks.uploadVertexBuffer(dispatch, source, target);
    }
}
