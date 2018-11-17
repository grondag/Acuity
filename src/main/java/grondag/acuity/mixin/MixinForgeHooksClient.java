package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraftforge.client.ForgeHooksClient;

@Mixin(ForgeHooksClient.class)
public abstract class MixinForgeHooksClient
{
    private static ThreadLocal<MutableBlockPos> posAdd = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up during chunk load
    @Redirect(method = "getSkyBlendColour", expect = 1, at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"))
    private static BlockPos onGetSkyBlendColourPosAdd(BlockPos pos, int x, int y, int z)
    {
        return posAdd.get().setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }
}
