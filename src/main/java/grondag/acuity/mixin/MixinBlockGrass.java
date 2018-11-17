package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.BlockGrass;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

@Mixin(BlockGrass.class)
public abstract class MixinBlockGrass
{
    private static ThreadLocal<MutableBlockPos> getUpPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "getActualState", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onGetActualStateUp(BlockPos pos)
    {
        return getUpPos.get().setPos(pos.getX(), pos.getY() + 1, pos.getZ());
    }
    
}
