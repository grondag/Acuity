package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.BlockLiquid;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid
{
    
    private static ThreadLocal<MutableBlockPos> shouldSideBeRenderedPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    private static ThreadLocal<MutableBlockPos> lightMapPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "shouldSideBeRendered", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onOffset(BlockPos pos, EnumFacing facing)
    {
        return shouldSideBeRenderedPos.get().setPos(pos).move(facing);
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "getPackedLightmapCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onUp(BlockPos pos)
    {
        return lightMapPos.get().setPos(pos.getX(), pos.getY() + 1, pos.getZ());
    }
}
