package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.chunk.Chunk;

@Mixin(Chunk.class)
public abstract class MixinChunk
{
    private static ThreadLocal<MutableBlockPos> getLightOpacityPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getBlockLightOpacity(III)I", expect = 1,
            at = @At(value = "NEW", args = "class=net/minecraft/util/math/BlockPos") )
    private BlockPos onBlockLightOpacityNewPos(int x, int y, int z)
    {
        return getLightOpacityPos.get().setPos(x, y, z);
    }
}
