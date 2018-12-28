package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.GlStateManager;

@Mixin(GlStateManager.class)
public class MixinGlStateManager
{
    @Shadow public static final MixinFogState FOG = null;
}
