package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.BlockRenderLayer;

@Environment(EnvType.CLIENT)
public interface IPipelinedQuad
{
    public IRenderPipeline getPipeline();

    /**
     * Quad must call {@link IPipelinedVertexConsumer#acceptVertex(IPipelinedVertex)} with
     * its vertex information.<p>
     * 
     * For tint, quad (or the model it comes from) is responsible for retrieving and applying block tint 
     * to the vertex colors.  This is done because lighter has no way to know which colors
     * should be modified when there is more than one color/texture layer. And many models don't use it.<p>
     * 
     * You can retrieve the block color from tint with {@link IPipelinedVertexConsumer#getBlockInfo()} 
     * and then {@link BlockInfo#getColorMultiplier(int tint)};
     */
    public void produceVertices(IPipelinedVertexConsumer vertexLighter);

    public BlockRenderLayer getRenderLayer();
}
