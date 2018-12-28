package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.LoadingConfig;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.util.math.BlockPos;

@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher
{
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockModelRenderer;renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Z)Z"))
    private boolean renderModel(BlockModelRenderer blockModelRenderer, IBlockAccess blockAccessIn, IBakedModel modelIn, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder buffer, boolean checkSides)
    {
        if(LoadingConfig.INSTANCE.enableBlockStats)
            return PipelineHooks.renderModelDebug(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
        else
            return PipelineHooks.renderModel(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
    }
    
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockFluidRenderer;renderFluid(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;)Z"))
    private boolean renderFluid(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        if(LoadingConfig.INSTANCE.enableFluidStats)
            return PipelineHooks.renderFluidDebug(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
        else
            return PipelineHooks.renderFluid(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
    }
}
