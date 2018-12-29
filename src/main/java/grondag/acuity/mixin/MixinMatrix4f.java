package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import grondag.acuity.mixin.extension.Matrix4fExt;
import net.minecraft.client.util.math.Matrix4f;

@Mixin(Matrix4f.class)
public abstract class MixinMatrix4f implements Matrix4fExt
{
    @Override
    @Accessor public abstract float[] getComponents();
    
    @Override
    public void setFromMatrix(Matrix4f source)
    {
        System.arraycopy(((Matrix4fExt)(Object)source).getComponents(), 0, getComponents(), 0, 16);
    }
    
    //PERF - add no-garbage multiply
    // may also be worth inlining the multiplication vs nested loops, tho hopefully HotSpot will cover it
}
