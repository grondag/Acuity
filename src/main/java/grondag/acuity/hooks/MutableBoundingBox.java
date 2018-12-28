package grondag.acuity.hooks;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;

public interface MutableBoundingBox
{
    MutableBoundingBox set(BoundingBox box);

    MutableBoundingBox growMutable(double value);

    MutableBoundingBox growMutable(double x, double y, double z);
    
    BoundingBox toImmutable();

    MutableBoundingBox offsetMutable(BlockPos pos);

    BoundingBox cast();

    MutableBoundingBox expandMutable(double x, double y, double z);

}
