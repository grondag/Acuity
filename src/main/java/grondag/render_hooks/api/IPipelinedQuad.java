package grondag.render_hooks.api;

public interface IPipelinedQuad
{
    public IRenderPipeline getPipeline();

    public int getTintIndex();
    
    public void produceVertices(IPipelinedVertexConsumer vertexLighter);
}
