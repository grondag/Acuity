package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;

@Mixin(BlockGrass.class)
public abstract class MixinBlockGrass
{
    private static ThreadLocal<BlockPos.Mutable> getUpPos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "getActualState", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onGetActualStateUp(BlockPos pos)
    {
        return getUpPos.get().set(pos.getX(), pos.getY() + 1, pos.getZ());
    }
    
}
