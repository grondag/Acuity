package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.Profiler;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public interface AccessMinecraftClient
{
    @Accessor
    public Profiler getProfiler();
}
