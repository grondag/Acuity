package grondag.acuity.hooks;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

public class BlockHooks
{
    private static ThreadLocal<MutableBlockPos> offsetPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    public static BlockPos onOffset(BlockPos pos, EnumFacing facing)
    {
        return offsetPos.get().setPos(pos).move(facing);
    }
}
