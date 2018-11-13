package grondag.acuity.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;

import grondag.acuity.hooks.ISetVisibility;
import grondag.acuity.hooks.VisibilityHooks;
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
        releaseVisibilityData();
        visibilityData = data;
    }
    
    /** reuse arrays to prevent garbage build up */
    private void releaseVisibilityData()
    {
        Object prior = visibilityData;
        if(prior instanceof byte[])
        {
            VisibilityHooks.releaseVisibilityMap((byte[]) prior);
            visibilityData = null;
        }
    }
    
    @Override
    protected void finalize()
    {
        releaseVisibilityData();
    }
}
