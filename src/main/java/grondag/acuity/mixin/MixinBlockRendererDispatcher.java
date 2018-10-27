package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.AcuityCore;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

@Mixin(BlockRendererDispatcher.class)
public class MixinBlockRendererDispatcher
{
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockModelRenderer;renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Z)Z"))
    private boolean renderModel(BlockModelRenderer blockModelRenderer, IBlockAccess blockAccessIn, IBakedModel modelIn, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder buffer, boolean checkSides)
    {
        if(AcuityCore.config.enableBlockStats)
            return PipelineHooks.renderModelDebug(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
        else
            return PipelineHooks.renderModel(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
    }
    
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockFluidRenderer;renderFluid(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;)Z"))
    private boolean renderFluid(BlockFluidRenderer fluidRenderer, IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn, BufferBuilder bufferBuilderIn)
    {
        if(AcuityCore.config.enableFluidStats)
            return PipelineHooks.renderFluidDebug(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
        else
            return PipelineHooks.renderFluid(fluidRenderer, blockAccess, blockStateIn, blockPosIn, bufferBuilderIn);
    }
}
