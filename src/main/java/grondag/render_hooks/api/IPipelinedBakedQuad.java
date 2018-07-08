package grondag.render_hooks.api;

public interface IPipelinedBakedQuad
{
    public IRenderPipeline getPipeline();

    public int getTintIndex();
    
    public boolean shouldApplyDiffuseLighting();
    
    public void produceVertices(IPipelinedVertexLighter vertexLighter);
}
