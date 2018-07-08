package grondag.render_hooks.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelinedQuad
{
    public IRenderPipeline getPipeline();

    public int getTintIndex();
    
    public void produceVertices(IPipelinedVertexConsumer vertexLighter);
}
