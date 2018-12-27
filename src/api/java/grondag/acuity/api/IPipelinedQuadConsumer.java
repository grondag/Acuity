package grondag.acuity.api;

import java.util.function.Consumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

@Environment(EnvType.CLIENT)
public interface IPipelinedQuadConsumer extends Consumer<IPipelinedQuad>
{
    /**
     * If your model has quads co-planar with a block face, then you should
     * exclude them if this method returns false for that face.<p>
     * 
     * This is in lieu of the getQuads(EnumFacing) pattern used in IBakedQuad.
     */
    public boolean shouldOutputSide(Direction side);
    
    /**
     * If your model already segregates quads by block layer you can reduce 
     * processing time by passing only quads in this layer to the consumer.<p>
     * 
     * If your model just has one big list of quads, you can simply pass them all to the consumer.
     * The consumer still checks and will skip quads not in the target layer.
     */
    public BlockRenderLayer targetLayer();
    
    /**
     * Provides access to in-world block position for model customization.
     */
    public BlockPos pos();
    
    /**
     * Provides access to block world for model customization.
     */
    public ExtendedBlockView world();
    
    /**
     * Provides access to block state for model customization.<br>
     * Is what normally is passed to IBakedModel and may be an IExtendedBlockState.
     */
    public BlockState blockState();
    
    /**
     * Deterministically pseudo-random bits based on block position.<br>
     * Will be same as what normally is passed to IBakedModel but is computed
     * lazily - will not be calculated if never retrieved.
     */
    public long positionRandom();
    
}
