package grondag.acuity.mixin;

import java.util.BitSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.extension.AcuityChunkVisibility;
import grondag.acuity.hooks.VisibilityMap;
import net.minecraft.client.render.chunk.ChunkVisibility;

@Mixin(ChunkVisibility.class)
public abstract class MixinChunkVisibility implements AcuityChunkVisibility
{
    @Shadow private BitSet bitSet;
    
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
    
    @Override
    public void clear()
    {
        setVisibilityData(null);
        bitSet.clear();
    }
}
