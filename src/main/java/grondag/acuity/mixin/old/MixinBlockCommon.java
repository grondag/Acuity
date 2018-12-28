package grondag.acuity.mixin.old;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.acuity.hooks.MutableBoundingBox;
import net.minecraft.block.Block;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.BlockPos;

@Mixin(Block.class)
public abstract class MixinBlockCommon
{
    private static ThreadLocal<MutableBoundingBox> mutableAABB = new ThreadLocal<MutableBoundingBox>()
    {
        @Override
        protected MutableBoundingBox initialValue()
        {
            return (MutableBoundingBox)new BoundingBox(0, 0, 0, 0, 0, 0);
        }
    };
    
    /**
     * @reason Use threadlocal AABB for intersection test to prevent garbage.  Same logic as vanilla.
     * @author grondag
     */
    @Overwrite
    protected static void addCollisionBoxToList(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, AxisAlignedBB blockBox)
    {
        if (blockBox != Block.NULL_AABB)
        {
            AxisAlignedBB axisalignedbb = mutableAABB.get().set(blockBox).offsetMutable(pos).cast();

            if (entityBox.intersects(axisalignedbb))
            {
                collidingBoxes.add(((MutableBoundingBox)axisalignedbb).toImmutable());
            }
        }
    }
}
