package grondag.acuity.core;

import java.nio.ByteBuffer;

import grondag.acuity.api.RenderPipeline;

public abstract class AbstractVertexPackingRenderer
{
    abstract public void accept(RenderPipeline pipeline, int vertexCount);
    
    abstract public int size();

    abstract public int quadCount();

    public abstract void upload(ByteBuffer buffer, VertexPackingList packing);

    public abstract void deleteGlResources();

    public abstract void render(boolean isSolidLayer);
}