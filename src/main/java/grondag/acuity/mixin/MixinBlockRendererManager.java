package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.LoadingConfig;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

@Mixin(BlockRenderManager.class)
public abstract class MixinBlockRendererManager
{
    @Redirect(method = "tesselateBlock", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/render/block/BlockRenderManager;tesselate(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;ZLjava/util/Random;J)Z"))
    private boolean renderModel(BlockModelRenderer blockModelRenderer, ExtendedBlockView blockAccessIn, BakedModel modelIn, BlockState blockStateIn, BlockPos blockPosIn, BufferBuilder buffer, boolean checkSides)
    {
        if(LoadingConfig.INSTANCE.enableBlockStats)
            return PipelineHooks.renderModelDebug(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
        else
            return PipelineHooks.renderModel(blockModelRenderer, blockAccessIn, modelIn, blockStateIn, blockPosIn, buffer, checkSides);
    }
    
    @Redirect(method = "tesselateFluid", at = @At(value = "INVOKE", 
            target = "net/minecraft/client/render/block/FluidRenderer;tesselate(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;Lnet/minecraft/fluid/FluidState;)Z"))
    
    
    private boolean renderFluid(FluidRenderer fluidRenderer, ExtendedBlockView blockAccess, BlockPos blockPosIn, BufferBuilder bufferBuilderIn, FluidState fluidStateIn)
    {
        if(LoadingConfig.INSTANCE.enableFluidStats)
            return PipelineHooks.renderFluidDebug(fluidRenderer, blockAccess, fluidStateIn, blockPosIn, bufferBuilderIn);
        else
            return PipelineHooks.renderFluid(fluidRenderer, blockAccess, fluidStateIn, blockPosIn, bufferBuilderIn);
    }
}
