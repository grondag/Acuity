package grondag.render_hooks.core;

import grondag.render_hooks.api.IPipelinedBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class PipelineHooks
{
    public static boolean renderModel(net.minecraft.client.renderer.BlockModelRenderer blockModelRenderer, IBlockAccess blockAccess, IBakedModel model, IBlockState state, BlockPos pos,
            BufferBuilder bufferBuilderIn, boolean checkSides)
    {
        if(model instanceof IPipelinedBakedModel)
            return PipelinedBlockModelRenderer.renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
        else
            return blockModelRenderer.renderModel(blockAccess, model, state, pos, bufferBuilderIn, checkSides);
    }

}
