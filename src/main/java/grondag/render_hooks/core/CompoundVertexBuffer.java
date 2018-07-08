package grondag.render_hooks.core;

import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class CompoundVertexBuffer extends VertexBuffer
{
    public CompoundVertexBuffer(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);
    }

    @Override
    public void deleteGlBuffers()
    {
        // TODO delete child buffers
        super.deleteGlBuffers();
    }
}
