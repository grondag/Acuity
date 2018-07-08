package grondag.render_hooks.api;

import java.util.function.Consumer;

import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;

public interface IPipelinedQuadConsumer extends Consumer<IPipelinedBakedQuad>
{
    /**
     * If your model has quads co-planar with a block face, then you should
     * exclude them if this method returns false for that face.<p>
     * 
     * This is in lieu of the getQuads(EnumFacing) pattern used in IBakedQuad.
     */
    public boolean shouldOutputSide(EnumFacing side);
    
    /**
     * If your model already segregates quads by block layer you can reduce 
     * processing time by passing only quads in this layer to the consumer.<p>
     * 
     * If your model just has one big list of quads, you can simply pass them all the consumer.
     * The consumer still checks and will skip quads not in the target layer.
     */
    public BlockRenderLayer targetLayer();
    
    public BlockPos pos();
    
    public IBlockAccess world();
    
    public IExtendedBlockState extendedState();
    
    public long positionRandom();
    
}
