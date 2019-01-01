package grondag.acuity.api.model;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block.OffsetType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ExtendedBlockView;

public abstract class BlockVertexConsumerImpl extends AbstractVertexConsumer implements BlockVertexConsumer
{
    protected BlockPos pos;
    protected ExtendedBlockView world;
    protected BlockState blockState;
    protected BlockEntity blockEntity;
    protected final Random rand = new Random();
    protected boolean doesRandomNeedInitalized = true;
    protected boolean enableOcclusionFilter = true;
    protected int sideLookupCompletionFlags = 0;
    protected int sideLookupResultFlags = 0;
    protected boolean isTranslucent = false;
    
    @Override
    public void clearSettings()
    {
        super.clearSettings();
        doesRandomNeedInitalized = true;
        enableOcclusionFilter = true;
        sideLookupCompletionFlags = 0;
        sideLookupResultFlags = 0;
        isTranslucent = false;
    }
    
    @Override
    public final BlockPos pos()
    {
        return pos;
    }

    @Override
    public final ExtendedBlockView world()
    {
        return world;
    }

    @Override
    public final BlockState blockState()
    {
        return blockState;
    }

    @Override
    public final Random random()
    {
        final Random rand = this.rand;
        if(doesRandomNeedInitalized)
        {
            rand.setSeed(blockState.getRenderingSeed(pos));
            doesRandomNeedInitalized = false;
        }
        return rand;
    }
    
    @Override
    public final void setAutomaticCullingEnabled(boolean isEnabled)
    {
        enableOcclusionFilter = isEnabled;
    }
    
    @Override
    public final boolean shouldDrawSide(Direction side)
    {
        final int mask = 1 << side.ordinal();
        if((sideLookupCompletionFlags & mask) == 0)
        {
            sideLookupCompletionFlags |= mask;
            boolean result = Block.shouldDrawSide(blockState, world, pos, side);
            if(result)
                sideLookupResultFlags |= mask;
            return result;
        }
        else
            return (sideLookupResultFlags & mask) != 0;
    }
    
    @Override
    public final BlockEntity blockEntity()
    {
        return blockEntity;
    }

    @Override
    public void setTranslucent(boolean isTranslucent)
    {
        this.isTranslucent = true;
        this.pipeline = null;
    }

    // PERF make BE lookup lazy
    public abstract BlockVertexConsumer prepare(BlockPos pos, ExtendedBlockView world, BlockState state, BlockEntity blockEntity);
        
    protected final void prepareInner(BlockPos pos, ExtendedBlockView world, BlockState state, BlockEntity blockEntity)
    {
        this.pos = pos;
        this.world = world;
        this.blockState = state;
        this.blockEntity = blockEntity;
        clearSettings();
        
        if(blockState.getBlock().getOffsetType() == OffsetType.NONE)
        {
            offsetX = 0;
            offsetY = 0;
            offsetZ = 0;
        }
        else
        {
            Vec3d offset = blockState().getOffsetPos(world, pos);
            offsetX = (float) offset.x;
            offsetY = (float) offset.y;
            offsetZ = (float) offset.z;
        }
    }
}
