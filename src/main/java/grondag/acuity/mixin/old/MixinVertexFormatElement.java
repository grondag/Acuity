package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

@Mixin(VertexFormatElement.class)
public abstract class MixinVertexFormatElement
{   
    @Redirect(method = "<init>*", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/vertex/VertexFormatElement;isFirstOrUV(ILnet/minecraft/client/renderer/vertex/VertexFormatElement$EnumUsage;)Z"))
    private boolean onIsFirstOrUV(VertexFormatElement caller, int index, VertexFormatElement.EnumUsage usage)
    {
        return PipelineHooks.isFirstOrUV(index, usage);
    }
}