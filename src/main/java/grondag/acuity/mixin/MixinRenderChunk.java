package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.core.CompoundVertexBuffer;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;

@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk
{
    @Redirect(method = "<init>*", require = 1, 
            at = @At(value = "NEW", args = "class=net/minecraft/client/renderer/vertex/VertexBuffer") )
    private VertexBuffer newVertexBufffer(VertexFormat format)
    {
        return new CompoundVertexBuffer(format);
    }
    
    @Redirect(method = "setPosition", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;initModelviewMatrix()V"))
    private void onInitModelviewMatrix(RenderChunk renderChunk)
    {
        PipelineHooks.renderChunkInitModelViewMatrix(renderChunk);
    }
}
