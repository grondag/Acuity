package grondag.acuity.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.buffering.DrawableChunk.Solid;
import grondag.acuity.buffering.DrawableChunk.Translucent;
import grondag.acuity.hooks.IRenderChunk;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.SetVisibility;

@Mixin(RenderChunk.class)
public class MixinRenderChunk implements IRenderChunk
{
    @Nullable Solid solidDrawable;
    @Nullable Translucent translucentDrawable;
    
    @Override
    public @Nullable Solid getSolidDrawable()
    {
        return solidDrawable;
    }

    @Override
    public @Nullable Translucent getTranslucentDrawable()
    {
        return translucentDrawable;
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
    
    @Inject(method = "deleteGlResources*", at = @At("RETURN"), require = 1)
    private void onDeleteGlResources(CallbackInfo ci)
    {
        releaseDrawables();
    }

    @Override
    public void setSolidDrawable(Solid drawable)
    {
        solidDrawable = drawable;
    }

    @Override
    public void setTranslucentDrawable(Translucent drawable)
    {
        translucentDrawable = drawable;
    }

    @Override
    public void releaseDrawables()
    {
        if(solidDrawable != null)
        {
            solidDrawable.clear();
            solidDrawable = null;
        }
        
        if(translucentDrawable != null)
        {
            translucentDrawable.clear();
            translucentDrawable = null;
        }
    }
}
