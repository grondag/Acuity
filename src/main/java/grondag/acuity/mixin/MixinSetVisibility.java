package grondag.acuity.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;

import grondag.acuity.hooks.ISetVisibility;
import net.minecraft.client.renderer.chunk.SetVisibility;

@Mixin(SetVisibility.class)
public abstract class MixinSetVisibility implements ISetVisibility
{
    private Object visibilityData = null;
    
    @Override
    public @Nullable Object getVisibilityData()
    {
        return visibilityData;
    }

    @Override
    public void setVisibilityData(Object data)
    {
        visibilityData = data;
    }
}
