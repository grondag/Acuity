package grondag.acuity.api;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelinedBakedModel extends IBakedModel
{
    /**
     * If your model has a performant way to know if it may have quads
     * in a given block layer, you can shortcut processing by overriding
     * this method.  Otherwise consumer will simply filter by quad.
     */
    public default boolean mightRenderInLayer(BlockRenderLayer forLayer)
    {
        return true;
    }
    
    /**
     * Default implementation simply casts IBakedModel getQuads() output and routes to consumer.
     * Some model implementations could be more efficient and/or want to do different things.<p>
     * 
     * If your model segregates quads by layer, query the provided consumer for render layer to improve efficiency.
     */
    public default void produceQuads(IPipelinedQuadConsumer quadConsumer)
    {
        produceQuadsInner(quadConsumer, null);
        produceQuadsInner(quadConsumer, EnumFacing.DOWN);
        produceQuadsInner(quadConsumer, EnumFacing.EAST);
        produceQuadsInner(quadConsumer, EnumFacing.NORTH);
        produceQuadsInner(quadConsumer, EnumFacing.SOUTH);
        produceQuadsInner(quadConsumer, EnumFacing.UP);
        produceQuadsInner(quadConsumer, EnumFacing.WEST);
    }
    
    public default void produceQuadsInner(IPipelinedQuadConsumer quadConsumer, @Nullable EnumFacing face)
    {
        if(face == null || quadConsumer.shouldOutputSide(face))
        {
            final List<BakedQuad> quads = this.getQuads(quadConsumer.blockState(), face, quadConsumer.positionRandom());
            final int limit = quads.size();
            if(limit == 0)
                return;
            for(int i = 0; i < limit; i++)
                quadConsumer.accept((IPipelinedQuad)quads.get(i));
        }
    }
}
