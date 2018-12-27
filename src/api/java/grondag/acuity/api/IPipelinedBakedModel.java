package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.client.render.model.BakedModel;

@Environment(EnvType.CLIENT)
public interface IPipelinedBakedModel extends BakedModel
{
    /**
     * If your model has a performant way to know if it may have quads
     * in a given block layer, you can shortcut processing by overriding
     * this method.  Otherwise consumer will simply filter by quad.
     */
    //UGLY: still need this?
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
    public void produceQuads(IPipelinedQuadConsumer quadConsumer);
   
}
