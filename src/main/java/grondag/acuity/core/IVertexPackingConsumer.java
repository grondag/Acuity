package grondag.acuity.core;

import grondag.acuity.api.RenderPipeline;

public interface IVertexPackingConsumer
{
    public void accept(RenderPipeline pipeline, int vertexCount);
}
