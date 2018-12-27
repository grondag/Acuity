package grondag.acuity.mixin.old;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;

import grondag.acuity.hooks.ISetVisibility;
import grondag.acuity.hooks.VisibilityMap;
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
    @Override
    public void releaseVisibilityData()
    {
        Object prior = visibilityData;
        if(prior != null && prior instanceof VisibilityMap)
        {
            VisibilityMap.release((VisibilityMap) prior);
            visibilityData = null;
        }
    }
    
    @Override
    protected void finalize()
    {
        releaseVisibilityData();
    }
}