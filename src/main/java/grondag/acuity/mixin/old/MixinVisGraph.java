package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.acuity.Acuity;
import grondag.acuity.hooks.VisibilityHooks;

@Mixin(VisGraph.class)
public abstract class MixinVisGraph
{   
    /**
     * Called from {@link RenderChunk#rebuildChunk(float, float, float, net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator)} to
     * store pre-computed visibility for use during render as a performance optimization.
     */
    @Inject(method = "computeVisibility", at = @At("HEAD"), cancellable = true, expect = 1)
    public void onComputeVisibility(CallbackInfoReturnable<SetVisibility> ci)
    {
        if(Acuity.isModEnabled())
            ci.setReturnValue(VisibilityHooks.computeVisiblityExt((VisGraph)(Object)this));
    }
}
