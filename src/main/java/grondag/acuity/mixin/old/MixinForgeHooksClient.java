package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;

@Mixin(ForgeHooksClient.class)
public abstract class MixinForgeHooksClient
{
    private static ThreadLocal<BlockPos.Mutable> posAdd = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getSkyBlendColour", expect = 1, at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"))
    private static BlockPos onGetSkyBlendColourPosAdd(BlockPos pos, int x, int y, int z)
    {
        return posAdd.get().set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }
}
