package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$FogState")
public interface AccessFogState
{
    @Accessor public int getMode();
    @Accessor public float getDensity();
    @Accessor public float getStart();
    @Accessor public float getEnd();
}
