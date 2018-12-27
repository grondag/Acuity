package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.util.concurrent.ListenableFuture;

import grondag.acuity.Acuity;
import grondag.acuity.api.AcuityRuntime;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.class_856;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Inject(method = "prepareTerrain", at = @At("HEAD"), cancellable = false, require = 1)
    void onPrepareTerrain(Entity cameraEntity, float fractionalTicks, class_856 class_856_1, int int_1, boolean boolean_1)
    {
        PipelineManager.INSTANCE.prepareForFrame(cameraEntity, fractionalTicks);
    }
    
    @Inject(method = "reload", at = @At("HEAD"), cancellable = false, require = 1)
    void onReload(CallbackInfo ci)
    {
        AcuityRuntime.INSTANCE.forceReload();
    }
}
