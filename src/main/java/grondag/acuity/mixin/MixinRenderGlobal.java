package grondag.acuity.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.acuity.Acuity;
import grondag.acuity.core.SetVisibilityExt;
import grondag.acuity.hooks.VisiblityHooks;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal
{
    @Shadow private ViewFrustum viewFrustum;
    
    /**
     * Called from {@link RenderGlobal#setupTerrain(net.minecraft.entity.Entity, double, net.minecraft.client.renderer.culling.ICamera, int, boolean)}.
     * Relies on pre-computed visibility stored during render chunk rebuild vs computing on fly each time.
     */
    @Inject(method = "getVisibleFacings", at = @At("HEAD"), cancellable = true, expect = 1)
    public void onGetVisibleFacings(BlockPos eyePos, CallbackInfoReturnable<Set<EnumFacing>> ci)
    {
        if(Acuity.isModEnabled())
        {
            RenderChunk renderChunk = viewFrustum.getRenderChunk(eyePos);
            if(renderChunk != null)
            {
                SetVisibility rawVis = renderChunk.compiledChunk.setVisibility;
                // unbuilt chunks won't have extended info
                if(rawVis instanceof SetVisibilityExt)
                {
                    ci.setReturnValue(VisiblityHooks.getVisibleFacingsExt((SetVisibilityExt)rawVis, eyePos));
                }
            }
        }
    }

}
