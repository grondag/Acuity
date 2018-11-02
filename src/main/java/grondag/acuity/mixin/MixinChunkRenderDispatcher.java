package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.util.concurrent.ListenableFuture;

import grondag.acuity.Acuity;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRenderDispatcher
{
    @Inject(method = "uploadChunk", at = @At("HEAD"), cancellable = true, require = 1)
    public void onUploadChunk(final BlockRenderLayer blockRenderLayer, final BufferBuilder bufferBuilder, final RenderChunk renderChunk, 
            final CompiledChunk compiledChunk, final double distanceSq, CallbackInfoReturnable<ListenableFuture<Object>> ci)
    {
        if(Acuity.isModEnabled())
            ci.setReturnValue(PipelineHooks.uploadChunk((ChunkRenderDispatcher)(Object)this, blockRenderLayer, bufferBuilder, renderChunk,
                    compiledChunk, distanceSq));
    }
}
