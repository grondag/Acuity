package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;

import grondag.acuity.hooks.ISetVisibility;
import grondag.acuity.hooks.VisibilityMap;

@Mixin(SetVisibility.class)
public abstract class MixinSetVisibility implements ISetVisibility
{
    private Object visibilityData = null;
    
    @Override
    public Object getVisibilityData()
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
