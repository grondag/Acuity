package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.LoadingConfig;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher
{
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockModelRenderer;renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Z)Z"))
    private boolean renderModel(BlockModelRenderer blockModelRenderer, ExtendedBlockView blockAccessIn, BakedModel modelIn, BlockState blockStateIn, BlockPos blockPosIn, BufferBuilder buffer, boolean checkSides)
    {
        if(LoadingConfig.INSTANCE.enableBlockStats)
            return PipelineHooks.renderModelDebug(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
        else
            return PipelineHooks.renderModel(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
    }
    
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockFluidRenderer;renderFluid(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;)Z"))
    private boolean renderFluid(FluidRenderer fluidRenderer, ExtendedBlockView blockAccess, BlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        if(LoadingConfig.INSTANCE.enableFluidStats)
            return PipelineHooks.renderFluidDebug(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
        else
            return PipelineHooks.renderFluid(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
    }
}
