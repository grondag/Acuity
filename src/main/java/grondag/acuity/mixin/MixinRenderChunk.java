package grondag.acuity.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.buffering.IDrawableChunk.Solid;
import grondag.acuity.buffering.IDrawableChunk.Translucent;
import grondag.acuity.core.CompoundVertexBuffer;
import grondag.acuity.hooks.IRenderChunk;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk implements IRenderChunk
{
    @Nullable Solid solidDrawable;
    @Nullable Translucent translucentDrawable;
    
    @Override
    public Solid getSolidDrawable()
    {
        Solid result = solidDrawable;
        if(result == null)
        {
            result = new CompoundVertexBuffer(DefaultVertexFormats.BLOCK);
            solidDrawable = result;
        }
        return result;
    }

    @Override
    public Translucent getTranslucentDrawable()
    {
        Translucent result = translucentDrawable;
        if(result == null)
        {
            result = new CompoundVertexBuffer(DefaultVertexFormats.BLOCK);
            translucentDrawable = result;
        }
        return result;
    }
    
    @Redirect(method = "setPosition", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;initModelviewMatrix()V"))
    private void onInitModelviewMatrix(RenderChunk renderChunk)
    {
        PipelineHooks.renderChunkInitModelViewMatrix(renderChunk);
    }
    
    @Redirect(method = "rebuildChunk", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;setVisibility(Lnet/minecraft/client/renderer/chunk/SetVisibility;)V"))
    private void onInitModelviewMatrix(CompiledChunk compiledChunk, SetVisibility setVisibility)
    {
        compiledChunk.setVisibility(setVisibility);
        PipelineHooks.mergeRenderLayers(compiledChunk);
    }
}
