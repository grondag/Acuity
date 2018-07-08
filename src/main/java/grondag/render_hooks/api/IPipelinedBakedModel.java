package grondag.render_hooks.api;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

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
     * Some model implementations could be more efficient and/or want to do different things.
     */
    public default void produceQuads(IPipelinedQuadConsumer quadConsumer)
    {
        this.getQuads(quadConsumer.extendedState(), null, quadConsumer.positionRandom()).forEach(q -> quadConsumer.accept((IPipelinedQuad)q));
        for(EnumFacing face : EnumFacing.VALUES)
        {
            if(quadConsumer.shouldOutputSide(face))
                this.getQuads(quadConsumer.extendedState(), face, quadConsumer.positionRandom()).forEach(q -> quadConsumer.accept((IPipelinedQuad)q));
        }
    }
}
