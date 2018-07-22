package grondag.render_hooks.core;

import net.minecraft.util.math.MathHelper;

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
        data = new int[MathHelper.smallestEncompassingPowerOfTwo(initialCapacity)];
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
    
    @Override
    public VertexCollector clone()
    {
        VertexCollector result = new VertexCollector(this.data.length);
        System.arraycopy(this.data, 0, result.data, 0, this.size);
        result.size = this.size;
        return result;
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
