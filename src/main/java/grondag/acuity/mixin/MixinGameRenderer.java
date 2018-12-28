package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.extension.AcuityGameRenderer;
import net.minecraft.client.render.FogHelper;
import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements AcuityGameRenderer
{

    @Shadow private FogHelper fogHelper;
    
    @Override
    public FogHelper fogHelper()
    {
        return fogHelper;
    }
}
