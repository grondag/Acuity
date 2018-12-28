package grondag.acuity.mixin.old;

import java.util.Iterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.google.common.collect.Iterators;

import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.class_2353;

@Mixin(class_2353.class)
public abstract class MixinPlane
{
    /**
     * @reason Use static array instance for iterator to avoid making garbage.
     * @author grondag
     */
    @Overwrite
    public Iterator<Direction> iterator()
    {
        switch ((class_2353)(Object)this)
        {
            case HORIZONTAL:
                return Iterators.<Direction>forArray(PipelineHooks.HORIZONTAL_FACES);
                
            case VERTICAL:
                return Iterators.<Direction>forArray(PipelineHooks.VERTICAL_FACES);
                
            default:
                throw new Error("Someone's been tampering with the universe!");
        }
        
    }
}
