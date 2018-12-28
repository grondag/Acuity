package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid
{
    
    private static ThreadLocal<BlockPos.Mutable> shouldSideBeRenderedPos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    private static ThreadLocal<BlockPos.Mutable> lightMapPos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "shouldSideBeRendered", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onOffset(BlockPos pos, Direction facing)
    {
        return shouldSideBeRenderedPos.get().set(pos).move(facing);
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "getPackedLightmapCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onUp(BlockPos pos)
    {
        return lightMapPos.get().set(pos.getX(), pos.getY() + 1, pos.getZ());
    }
}
