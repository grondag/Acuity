package grondag.acuity.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.hooks.IMutableAxisAlignedBB;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

@Mixin(Entity.class)
public abstract class MixinEntity
{
    private static ThreadLocal<IMutableAxisAlignedBB> mutableAABB = new ThreadLocal<IMutableAxisAlignedBB>()
    {
        @Override
        protected IMutableAxisAlignedBB initialValue()
        {
            return (IMutableAxisAlignedBB)new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        }
    };
    
    // prevents significant garbage build up
    @Redirect(method = "isInLava",
            at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/AxisAlignedBB;grow(DDD)Lnet/minecraft/util/math/AxisAlignedBB;"))
    private AxisAlignedBB onIsInLava(AxisAlignedBB box, double dx, double dy, double dz)
    {
        return mutableAABB.get().set(box).growMutable(dx, dy, dz).cast();
    }
    
    // prevents significant garbage build up
    @Redirect(method = "handleWaterMovement",
            at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/AxisAlignedBB;grow(DDD)Lnet/minecraft/util/math/AxisAlignedBB;"))
    private AxisAlignedBB onHandleWaterMovement1(AxisAlignedBB box, double dx, double dy, double dz)
    {
        return mutableAABB.get().set(box).growMutable(dx - 0.001D, dy - 0.001D, dz - 0.001D).cast();
    }
    
    @Redirect(method = "handleWaterMovement",
            at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/AxisAlignedBB;shrink(D)Lnet/minecraft/util/math/AxisAlignedBB;"))
    private AxisAlignedBB onHandleWaterMovement2(AxisAlignedBB box, double d)
    {
        // handled in grow hook
        return box;
    }
}
