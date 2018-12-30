package grondag.acuity.core;


import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.AcuityVertexConsumer;
import grondag.acuity.api.PipelineManagerImpl;
import grondag.acuity.api.RenderPipelineImpl;
import grondag.acuity.fermion.varia.DirectionHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ExtendedBlockView;

@Environment(EnvType.CLIENT)
public abstract class CompoundVertexLighter implements AcuityVertexConsumer
{
    protected final IBlockInfo blockInfo;
    
    private PipelinedVertexLighter[] lighters = new PipelinedVertexLighter[PipelineManagerImpl.MAX_PIPELINES];

    protected BlockRenderLayer renderLayer;
    protected CompoundBufferBuilder target;
    protected boolean didOutput;

    
    protected BlockState blockState;
    
    protected long positionRandom = Long.MIN_VALUE;
    protected int sideFlags;
    
    public void prepare(CompoundBufferBuilder target, BlockRenderLayer layer, ExtendedBlockView world, BlockState blockState, BlockPos pos, boolean checkSides)
    {
        this.renderLayer = layer;
        this.target = target;
        this.didOutput = false;
        this.positionRandom = Long.MIN_VALUE;
        this.blockInfo.prepare(world, blockState, pos);
        this.blockState = blockState;
        this.sideFlags = checkSides ? getSideFlags() : 0xFFFF;
    }
    
    @SuppressWarnings("null")
    private int getSideFlags()
    {
        int result = 0;
        for(int f = 0; f < 6; f++)
        {
            final Direction face = DirectionHelper.fromOrdinal(f);
            if(this.blockState.shouldSideBeRendered(this.world(), this.pos(), face))
                result |= (1 << face.ordinal());
        }
        return result;
    }
    
    @Override
    public void accept(IPipelinedQuad quad)
    {
        if(quad != null && quad.getRenderLayer() == this.renderLayer)
            getPipelineLighter((RenderPipelineImpl)quad.getPipeline()).acceptQuad(quad);
    }
    
    public CompoundVertexLighter()
    {
        this.blockInfo = (IBlockInfo)(new BlockInfo(MinecraftClient.getInstance().getBlockColorMap()));
    }
    
    protected abstract PipelinedVertexLighter createChildLighter(RenderPipelineImpl pipeline);

    private PipelinedVertexLighter getPipelineLighter(RenderPipelineImpl pipeline)
    {
        if(pipeline == null)
            pipeline = PipelineManagerImpl.INSTANCE.defaultSinglePipeline;
        
        @SuppressWarnings("null")
        PipelinedVertexLighter result = lighters[pipeline.getIndex()];
        if(result == null)
        {
            result = createChildLighter(pipeline);
            lighters[pipeline.getIndex()] = result;
        }
        return result;
    }
    
    public boolean didOutput()
    {
        return this.didOutput;
    }

    @Override
    public boolean shouldOutputSide(Direction side)
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
        return this.blockInfo.blockPos();
    }

    @Override
    public final ExtendedBlockView world()
    {
        return this.blockInfo.world();
    }

    @Override
    public BlockState blockState()
    {
        return this.blockState;
    }

    @Override
    public long positionRandom()
    {
        long result = this.positionRandom;
        if(result == Long.MIN_VALUE)
        {
            result = MathHelper.getPositionRandom(this.blockInfo.blockPos());
            this.positionRandom = result;
        }
        return result;
    }
}
