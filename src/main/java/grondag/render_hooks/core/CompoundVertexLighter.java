package grondag.render_hooks.core;

import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IPipelinedBakedQuad;
import grondag.render_hooks.api.IPipelinedQuadConsumer;
import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.common.property.IExtendedBlockState;

public class CompoundVertexLighter implements IPipelinedQuadConsumer
{
    protected final BlockInfo blockInfo;
    
    private PipelinedVertexLighter[] lighters = new PipelinedVertexLighter[IPipelineManager.MAX_PIPELINE_COUNT];

    private BlockRenderLayer renderLayer;
    private CompoundBufferBuilder target;
    private boolean didOutput;
    private boolean needsBlockInfoUpdate;
    private IExtendedBlockState extendedState;
    private long positionRandom = Long.MIN_VALUE;
    private int sideFlags;
    
    private class ChildLighter extends PipelinedVertexLighter
    {
        protected ChildLighter(IRenderPipeline pipeline)
        {
            super(pipeline);
        }

        @Override
        public final BlockInfo getBlockInfo()
        {
            if(needsBlockInfoUpdate)
            {
                blockInfo.updateShift();
                if(Minecraft.isAmbientOcclusionEnabled())
                    blockInfo.updateLightMatrix();
                else
                    blockInfo.updateFlatLighting();
            }
            return blockInfo;
        }
        
        @Override
        public final BufferBuilder getPipelineBuffer()
        {
            return target.getPipelineBuffer(this.pipeline);
        }
    }
    
    public void prepare(CompoundBufferBuilder target, BlockRenderLayer layer, IBlockAccess world, IExtendedBlockState extendedState, BlockPos pos, boolean checkSides)
    {
        this.renderLayer = layer;
        this.target = target;
        this.didOutput = false;
        this.needsBlockInfoUpdate = true;
        this.positionRandom = Long.MIN_VALUE;
        this.blockInfo.setWorld(world);
        this.blockInfo.setState(extendedState);
        this.blockInfo.setBlockPos(pos);
        this.extendedState = extendedState;
        this.sideFlags = checkSides ? getSideFlags() : 0xFFFF;
    }
    
    public void releaseResources()
    {
        this.blockInfo.reset();
    }
    
    private int getSideFlags()
    {
        int result = 0;
        for (EnumFacing face : EnumFacing.values())
        {
            if(this.extendedState.shouldSideBeRendered(this.world(), this.pos(), face))
                result |= (1 << face.ordinal());
        }
        return result;
    }
    
    @Override
    public void accept(IPipelinedBakedQuad quad)
    {
        IRenderPipeline p = quad.getPipeline();
        if(p.renderLayer() == this.renderLayer)
            getPipelineLighter(p).acceptQuad(quad);
    }
    
    public CompoundVertexLighter()
    {
        this.blockInfo = new BlockInfo(Minecraft.getMinecraft().getBlockColors());
    }

    private PipelinedVertexLighter getPipelineLighter(IRenderPipeline pipeline)
    {
        PipelinedVertexLighter result = lighters[pipeline.getIndex()];
        if(result == null)
        {
            result = new ChildLighter(pipeline);
            lighters[pipeline.getIndex()] = result;
        }
        return result;
    }
    
    public boolean didOutput()
    {
        return this.didOutput;
    }
    
    public long getPositionRandom()
    {
        if(this.positionRandom == Long.MIN_VALUE)
        {
            this.positionRandom = MathHelper.getPositionRandom(this.pos());
        }
        return this.positionRandom;
    }

    @Override
    public boolean shouldOutputSide(EnumFacing side)
    {
        return (this.sideFlags & (1 << side.ordinal())) != 0;
    }

    @Override
    public BlockRenderLayer targetLayer()
    {
        return this.renderLayer;
    }

    @Override
    public final BlockPos pos()
    {
        return this.blockInfo.getBlockPos();
    }

    @Override
    public final IBlockAccess world()
    {
        return this.blockInfo.getWorld();
    }

    @Override
    public IExtendedBlockState extendedState()
    {
        return this.extendedState;
    }

    @Override
    public long positionRandom()
    {
        return this.positionRandom;
    }
}
