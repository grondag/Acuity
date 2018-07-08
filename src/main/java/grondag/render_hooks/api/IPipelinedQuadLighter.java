package grondag.render_hooks.api;

import net.minecraft.client.renderer.vertex.VertexFormat;

public interface IPipelinedQuadLighter
{
    VertexFormat getVertexFormat();
    
    public void acceptQuad(IPipelinedBakedQuad quad);
}
