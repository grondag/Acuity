package grondag.acuity.mixin;

import java.util.Iterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.google.common.collect.Iterators;

import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Plane;

@Mixin(Plane.class)
public abstract class MixinPlane
{
    // don't make garbage for no reason
    @Overwrite
    public Iterator<EnumFacing> iterator()
    {
        switch ((Plane)(Object)this)
        {
            case HORIZONTAL:
                return Iterators.<EnumFacing>forArray(PipelineHooks.HORIZONTAL_FACES);
                
            case VERTICAL:
                return Iterators.<EnumFacing>forArray(PipelineHooks.VERTICAL_FACES);
                
            default:
                throw new Error("Someone's been tampering with the universe!");
        }
        
    }
}
