package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.core.CompoundBufferBuilder;
import grondag.acuity.hooks.PipelineHooks;

@Mixin(RegionRenderCacheBuilder.class)
public abstract class MixinRegionRenderCacheBuilder
{
    @Redirect(method = "<init>*", require = 4, 
            at = @At(value = "NEW", args = "class=net/minecraft/client/renderer/BufferBuilder") )
    private BufferBuilder newBuferBuilder(int bufferSizeIn)
    {
        return new CompoundBufferBuilder(bufferSizeIn);
    }
    
    @Inject(method = "<init>*", require = 1, at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci)
    {
        PipelineHooks.linkBuilders((RegionRenderCacheBuilder)(Object)this);
    }
}
