package grondag.acuity.api.model;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

public interface BlockModelInputs
{
    /**
     * In-world block position for model customization.<p>
     * 
     * This may be a mutable instance and you should avoid retaining a reference to it.
     */
    public BlockPos pos();
    
    /**
     * Block world access for model customization.<p>
     * 
     * This will likely be a cached view and you should avoid retaining a reference to it.
     */
    public ExtendedBlockView world();
    
    /**
     * Block state for model customization.<p>
     * 
     * Will be same as what you'd get from {@link #world()} but this method will be more performant.
     */
    public BlockState blockState();
    
    /**
     * Block Entity if block has one. Null if not.<p>
     * 
     * Will be same as what you'd get from {@link #world()} but this method will be more performant.
     */
    public BlockEntity blockEntity();
    
    /**
     * Will be deterministically initialized based on block pos. using same logic as 
     * what is normally passed to getQuads but handled lazily - will not be initialized if never retrieved.
     */
    public Random random();
    
    /**
     * If your model has quads that are co-planar with a block face then you should 
     * check this method and skip quads for sides when this method returns false. <p>
     * 
     * This method is only meaningful for in-world block rendering and will always return true for item rendering
     * and other rendering scenarios where face occlusion doesn't apply.
     */
    public boolean shouldDrawSide(Direction side);
}
