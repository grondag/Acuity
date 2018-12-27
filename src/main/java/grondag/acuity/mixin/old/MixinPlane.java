package grondag.acuity.mixin.old;

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
    /**
     * @reason Use static array instance for iterator to avoid making garbage.
     * @author grondag
     */
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
