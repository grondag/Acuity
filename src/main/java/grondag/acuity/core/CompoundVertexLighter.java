package grondag.acuity.core;

import javax.annotation.Nullable;

import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IPipelinedQuadConsumer;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class CompoundVertexLighter implements IPipelinedQuadConsumer
{
    protected final LazyBlockInfo blockInfo;
    
    private PipelinedVertexLighter[] lighters = new PipelinedVertexLighter[PipelineManager.MAX_PIPELINES];

    protected @Nullable BlockRenderLayer renderLayer;
    protected CompoundBufferBuilder target;
    protected boolean didOutput;

    
    protected @Nullable IBlockState blockState;
    protected long positionRandom = Long.MIN_VALUE;
    protected int sideFlags;
    
    public void prepare(CompoundBufferBuilder target, BlockRenderLayer layer, IBlockAccess world, IBlockState blockState, BlockPos pos, boolean checkSides)
    {
        this.renderLayer = layer;
        this.target = target;
        this.didOutput = false;
        this.positionRandom = Long.MIN_VALUE;
        this.blockInfo.prepare(world, blockState, pos);
        this.blockInfo.updateShift();
        this.blockState = blockState;
        this.sideFlags = checkSides ? getSideFlags() : 0xFFFF;
    }
    
    @SuppressWarnings("null")
    private int getSideFlags()
    {
        int result = 0;
        for (EnumFacing face : EnumFacing.values())
        {
            if(this.blockState.shouldSideBeRendered(this.world(), this.pos(), face))
                result |= (1 << face.ordinal());
        }
        return result;
    }
    
    @Override
    public void accept(@Nullable IPipelinedQuad quad)
    {
        if(quad != null && quad.getRenderLayer() == this.renderLayer)
            getPipelineLighter((RenderPipeline)quad.getPipeline()).acceptQuad(quad);
    }
    
    @SuppressWarnings("null")
    public CompoundVertexLighter()
    {
        this.blockInfo = new LazyBlockInfo(Minecraft.getMinecraft().getBlockColors());
    }
    
    protected abstract PipelinedVertexLighter createChildLighter(RenderPipeline pipeline);

    private PipelinedVertexLighter getPipelineLighter(@Nullable RenderPipeline pipeline)
    {
        if(pipeline == null)
            pipeline = PipelineManager.INSTANCE.defaultSinglePipeline;
        
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
    public boolean shouldOutputSide(EnumFacing side)
    {
        return (this.sideFlags & (1 << side.ordinal())) != 0;
    }

    @Override
    @Nullable
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
    @Nullable
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
            result = MathHelper.getPositionRandom(this.blockInfo.getBlockPos());
            this.positionRandom = result;
        }
        return result;
    }
}
