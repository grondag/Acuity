package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import grondag.acuity.api.AcuityRuntimeImpl;
import grondag.acuity.api.PipelineManagerImpl;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BoundingBoxTest;

// PERF: restore visibility hooks if profiling shows worthwhile
// Computation is in class_852
// See forge branch MixinVisGraph.onComputeVisibility for details

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Inject(method = "prepareTerrain", at = @At("HEAD"), cancellable = false, require = 1)
    void onPrepareTerrain(Entity cameraEntity, float fractionalTicks, BoundingBoxTest class_856_1, int int_1, boolean boolean_1)
    {
        PipelineManagerImpl.INSTANCE.prepareForFrame(cameraEntity, fractionalTicks);
    }
    
    @Inject(method = "reload", at = @At("HEAD"), cancellable = false, require = 1)
    void onReload(CallbackInfo ci)
    {
        AcuityRuntimeImpl.INSTANCE.forceReload();
    }
}
