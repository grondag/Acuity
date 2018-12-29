package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.core.CompoundBufferBuilder;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;

@Mixin(BlockLayeredBufferBuilder.class)
public abstract class MixinBlockLayeredBufferBuilder
{
    @Redirect(method = "<init>*", require = 4, 
            at = @At(value = "NEW", args = "class=net/minecraft/client/render/BufferBuilder") )
    private BufferBuilder newBuferBuilder(int bufferSizeIn)
    {
        return new CompoundBufferBuilder(bufferSizeIn);
    }
    
    @Inject(method = "<init>*", require = 1, at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci)
    {
        PipelineHooks.linkBuilders((BlockLayeredBufferBuilder)(Object)this);
    }
}
