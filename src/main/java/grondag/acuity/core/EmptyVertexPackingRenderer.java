package grondag.acuity.core;

import java.nio.ByteBuffer;

import grondag.acuity.api.RenderPipeline;

public class EmptyVertexPackingRenderer extends AbstractVertexPackingRenderer
{
    public static final EmptyVertexPackingRenderer  INSTANCE = new EmptyVertexPackingRenderer();
    
    private EmptyVertexPackingRenderer()
    {
        
    }
    
    @Override
    public void accept(RenderPipeline pipeline, int vertexCount)
    {
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public int quadCount()
    {
        return 0;
    }

    @Override
    public void upload(ByteBuffer buffer, VertexPackingList packing)
    {
    }

    @Override
    public void deleteGlResources()
    {
    }

    @Override
    public void render(boolean isSolidLayer)
    {
    }
}
