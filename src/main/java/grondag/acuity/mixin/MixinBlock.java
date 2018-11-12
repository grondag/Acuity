package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.hooks.BlockHooks;
import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

@Mixin(Block.class)
public abstract class MixinBlock
{
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "shouldSideBeRendered", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onOffset(BlockPos pos, EnumFacing facing)
    {
        return BlockHooks.onOffset(pos, facing);
    }
}
