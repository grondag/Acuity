package grondag.acuity.core;

import java.nio.ByteBuffer;

public abstract class AbstractVertexPackingRenderer implements IVertexPackingConsumer
{
    abstract public int size();

    abstract public int quadCount();

    public abstract void upload(ByteBuffer buffer, VertexPackingList packing);

    public abstract void deleteGlResources();

    public abstract void render(boolean isSolidLayer);
}