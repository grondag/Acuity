package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

@Mixin(BlockFluidRenderer.class)
public abstract class MixinBlockFluidRenderer
{
    private static ThreadLocal<MutableBlockPos> getFluidHeightAddPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    private static ThreadLocal<MutableBlockPos> getFluidHeightUpPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    private static ThreadLocal<MutableBlockPos> renderFluidPos = new ThreadLocal<MutableBlockPos>()
    {
        @Override
        protected MutableBlockPos initialValue()
        {
            return new MutableBlockPos();
        }
    };
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "getFluidHeight", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onGetFluidHeightAdd(BlockPos pos, int x, int y, int z)
    {
        return getFluidHeightAddPos.get().setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "getFluidHeight", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onGetFluidHeightUp(BlockPos pos)
    {
        return getFluidHeightUpPos.get().setPos(pos.getX(), pos.getY() + 1, pos.getZ());
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "renderFluid", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onRenderFluidUp(BlockPos pos)
    {
        return renderFluidPos.get().setPos(pos.getX(), pos.getY() + 1, pos.getZ());
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "renderFluid", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;down()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onRenderFluidDown(BlockPos pos)
    {
        return renderFluidPos.get().setPos(pos.getX(), pos.getY() - 1, pos.getZ());
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "renderFluid", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;east()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onRenderFluidEast(BlockPos pos)
    {
        return renderFluidPos.get().setPos(pos.getX() + 1, pos.getY(), pos.getZ());
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "renderFluid", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;south()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onRenderFluidSouth(BlockPos pos)
    {
        return renderFluidPos.get().setPos(pos.getX(), pos.getY(), pos.getZ() + 1);
    }
    
    // prevents significant garbage build up during chunk rebuild
    @Redirect(method = "renderFluid", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onRenderFluidAdd(BlockPos pos, int x, int y, int z)
    {
        return renderFluidPos.get().setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }
}
