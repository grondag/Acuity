package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.FogHelper;

@Mixin(FogHelper.class)
public interface AccessFogHelper
{
    @Accessor public float getRed();
    @Accessor public float getGreen();
    @Accessor public float getBlue();
}
