package grondag.acuity.core;


import grondag.acuity.api.IBlockInfo;
import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IPipelinedQuadConsumer;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.fermion.varia.DirectionHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public abstract class CompoundVertexLighter implements IPipelinedQuadConsumer
{
    protected final IBlockInfo blockInfo;
    
    private PipelinedVertexLighter[] lighters = new PipelinedVertexLighter[PipelineManager.MAX_PIPELINES];

    protected BlockRenderLayer renderLayer;
    protected CompoundBufferBuilder target;
    protected boolean didOutput;

    
    protected BlockState blockState;
    
    protected long positionRandom = Long.MIN_VALUE;
    protected int sideFlags;
    
    public void prepare(CompoundBufferBuilder target, BlockRenderLayer layer, IBlockAccess world, IBlockState blockState, BlockPos pos, boolean checkSides)
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
            getPipelineLighter((RenderPipeline)quad.getPipeline()).acceptQuad(quad);
    }
    
    public CompoundVertexLighter()
    {
        this.blockInfo = (IBlockInfo)(new BlockInfo(Minecraft.getMinecraft().getBlockColors()));
    }
    
    protected abstract PipelinedVertexLighter createChildLighter(RenderPipeline pipeline);

    private PipelinedVertexLighter getPipelineLighter(RenderPipeline pipeline)
    {
        if(pipeline == null)
            pipeline = PipelineManager.INSTANCE.defaultSinglePipeline;
        
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
    public final IBlockAccess world()
    {
        return this.blockInfo.world();
    }

    @Override
    public IBlockState blockState()
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
