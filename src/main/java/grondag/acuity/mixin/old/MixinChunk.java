package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

@Mixin(Chunk.class)
public abstract class MixinChunk
{
    private static ThreadLocal<BlockPos.Mutable> getLightOpacityPos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getBlockLightOpacity(III)I", expect = 1,
            at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onBlockLightOpacityNewPos(int x, int y, int z)
    {
        return getLightOpacityPos.get().set(x, y, z);
    }
}
