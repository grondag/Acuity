package grondag.acuity.mixin.old;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.acuity.hooks.IMutableAxisAlignedBB;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

@Mixin(Block.class)
public abstract class MixinBlockCommon
{
    private static ThreadLocal<IMutableAxisAlignedBB> mutableAABB = new ThreadLocal<IMutableAxisAlignedBB>()
    {
        @Override
        protected IMutableAxisAlignedBB initialValue()
        {
            return (IMutableAxisAlignedBB)new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        }
    };
    
    /**
     * @reason Use threadlocal AABB for intersection test to prevent garbage.  Same logic as vanilla.
     * @author grondag
     */
    @Overwrite
    protected static void addCollisionBoxToList(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable AxisAlignedBB blockBox)
    {
        if (blockBox != Block.NULL_AABB)
        {
            @SuppressWarnings("null")
            AxisAlignedBB axisalignedbb = mutableAABB.get().set(blockBox).offsetMutable(pos).cast();

            if (entityBox.intersects(axisalignedbb))
            {
                collidingBoxes.add(((IMutableAxisAlignedBB)axisalignedbb).toImmutable());
            }
        }
    }
}
