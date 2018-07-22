package grondag.render_hooks.core;

public class VertexCollector
{
    private int[] data;
    private int size = 0;
    
    public VertexCollector()
    {
        this(128);
    }
    
    public VertexCollector(int initialCapacity)
    {
        data = new int[initialCapacity];
    }
    
    public int size()
    {
        return this.size;
    }
    
    public int[] rawData()
    {
        return this.data;
    }
    
    public void clear()
    {
        this.size = 0;
    }
    
    public final void add(final int i)
    {
        if (size == data.length)
        {
            final int copy[] = new int[size * 2];
            System.arraycopy(data, 0, copy, 0, size);
            data  = copy;
        }
        data[size++] = i;
    }
    
    public final void add(final float f)
    {
        this.add(Float.floatToRawIntBits(f));
    }
    
    public final void add(final double d)
    {
        this.add((float)d);
    }
}
