package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.hooks.IMutableAxisAlignedBB;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

@Mixin(World.class)
public abstract class MixinWorld
{
    private static ThreadLocal<IMutableAxisAlignedBB> mutableAABB = new ThreadLocal<IMutableAxisAlignedBB>()
    {
        @Override
        protected IMutableAxisAlignedBB initialValue()
        {
            return (IMutableAxisAlignedBB)new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        }
    };
    
    private static ThreadLocal<MutableBlockPos> mutablePos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up
    @Redirect(method = "getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;",
            at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/AxisAlignedBB;grow(D)Lnet/minecraft/util/math/AxisAlignedBB;"))
    private AxisAlignedBB onGetCBGrow(AxisAlignedBB box, double d)
    {
        return mutableAABB.get().set(box).growMutable(d).cast();
    }
    
    // prevents significant garbage build up
    @Redirect(method = "isFlammableWithin",
                      at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onIsFlammableWithin(int x, int y, int z) 
    {
        return mutablePos.get().setPos(x, y, z);
    }
}
