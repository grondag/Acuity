package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.mixin.extension.MinecraftClientExt;
import net.minecraft.class_3689;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements MinecraftClientExt
{
    @Shadow
    private class_3689 profiler;

    @Override
    public final class_3689 profiler()
    {
        return profiler;
    }

        
}
