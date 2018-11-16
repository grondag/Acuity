package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.BlockSnow;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

@Mixin(BlockSnow.class)
public class MixinBlockSnow
{
    private static ThreadLocal<MutableBlockPos> shouldSideBeRenderedPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "shouldSideBeRendered", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onOffset(BlockPos pos, EnumFacing facing)
    {
        return shouldSideBeRenderedPos.get().setPos(pos).move(facing);
    }
}
