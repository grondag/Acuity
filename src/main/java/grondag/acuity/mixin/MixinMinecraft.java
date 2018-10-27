package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.LoadingConfig;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
{
    //TODO: change to expect
    @Redirect(method = "runGameLoop", require = 1,
            at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"))
    private void onYield()
    {
        if(!LoadingConfig.INSTANCE.disableYieldInGameLoop)
            Thread.yield();
    }
}
