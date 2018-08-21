package grondag.acuity.core;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;

public class RendererStore
{
    private static final ObjectArrayFIFOQueue<AbstractVertexPackingRenderer> renderers = new ObjectArrayFIFOQueue<>();
    
    public static AbstractVertexPackingRenderer claim()
    {
        if(renderers.size() < 8)
            return new VertexPackingRenderer();
        else
            return renderers.dequeue();
    }
    
    public static void release(AbstractVertexPackingRenderer renderer)
    {
        renderers.enqueue(renderer);
    }

}
