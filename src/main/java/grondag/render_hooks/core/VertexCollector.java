package grondag.render_hooks.core;

import com.google.common.primitives.Floats;

import grondag.render_hooks.api.RenderPipeline;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import net.minecraft.util.math.MathHelper;

public class VertexCollector
{
    private int[] data;
    private int integerSize = 0;
    private RenderPipeline pipeline;
    
    public VertexCollector()
    {
        this(128);
    }
    
    @SuppressWarnings("null")
    public VertexCollector(int initialCapacity)
    {
        data = new int[MathHelper.smallestEncompassingPowerOfTwo(initialCapacity)];
    }
    
    public void prepare(RenderPipeline pipeline)
    {
        this.integerSize = 0;
        this.pipeline = pipeline;
    }
    
    public int byteSize()
    {
        return this.integerSize * 4;
    }
    
    public int integerSize()
    {
        return this.integerSize;
    }
    
    public int vertexCount()
    {
        return this.integerSize * 4 / this.pipeline.piplineVertexFormat().stride;
    }
    
    public int[] rawData()
    {
        return this.data;
    }
    
    @Override
    public VertexCollector clone()
    {
        VertexCollector result = new VertexCollector(this.data.length);
        System.arraycopy(this.data, 0, result.data, 0, this.integerSize);
        result.integerSize = this.integerSize;
        result.pipeline = this.pipeline;
        return result;
    }
    
    public final void add(final int i)
    {
        if (integerSize == data.length)
        {
            final int copy[] = new int[integerSize * 2];
            System.arraycopy(data, 0, copy, 0, integerSize);
            data  = copy;
        }
        data[integerSize++] = i;
    }
    
    public final void add(final float f)
    {
        this.add(Float.floatToRawIntBits(f));
    }
    
    public final void add(final double d)
    {
        this.add((float)d);
    }
    
    @SuppressWarnings("serial")
    public void sortQuads(float x, float y, float z)
    {
        // works because 4 bytes per int
        final int quadIntStride = this.pipeline.piplineVertexFormat().stride;
        final int vertexIntStride = quadIntStride / 4;
        final int quadCount = this.vertexCount() / 4;
        final float[] perQuadDistance = new float[quadCount];
        final int[] quadSwap = new int[quadIntStride];
        
        for (int j = 0; j < quadCount; ++j)
        {
            perQuadDistance[j] = getDistanceSq(x, y, z, vertexIntStride, j);
        }

        // sort the indexes by distance
        it.unimi.dsi.fastutil.Arrays.quickSort(0, quadCount, 
        new AbstractIntComparator()
        {
            @Override
            public int compare(int a, int b)
            {
                return Floats.compare(perQuadDistance[b], perQuadDistance[a]);
            }
        },
        new Swapper()
        {
            @Override
            public void swap(int a, int b)
            {
                float distSwap = perQuadDistance[a];
                perQuadDistance[a] = perQuadDistance[b];
                perQuadDistance[b] = distSwap;
                
                System.arraycopy(data, a * quadIntStride, quadSwap, 0, quadIntStride);
                System.arraycopy(data, b * quadIntStride, data, a * quadIntStride, quadIntStride);
                System.arraycopy(quadSwap, 0, data, b * quadIntStride, quadIntStride);
            }
        });
    }
    
    private float getDistanceSq(float x, float y, float z, int integerStride, int vertexIndex)
    {
        // unpack vertex coordinates
        int i = vertexIndex * integerStride * 4;
        float x0 = Float.intBitsToFloat(this.data[i]);
        float y0 = Float.intBitsToFloat(this.data[i + 1]);
        float z0 = Float.intBitsToFloat(this.data[i + 2]);
        
        i += integerStride;
        float x1 = Float.intBitsToFloat(this.data[i]);
        float y1 = Float.intBitsToFloat(this.data[i + 1]);
        float z1 = Float.intBitsToFloat(this.data[i + 2]);
        
        i += integerStride;
        float x2 = Float.intBitsToFloat(this.data[i]);
        float y2 = Float.intBitsToFloat(this.data[i + 1]);
        float z2 = Float.intBitsToFloat(this.data[i + 2]);
        
        i += integerStride;
        float x3 = Float.intBitsToFloat(this.data[i]);
        float y3 = Float.intBitsToFloat(this.data[i + 1]);
        float z3 = Float.intBitsToFloat(this.data[i + 2]);
        
        // compute average distance by component
        float dx = (x0 + x1 + x2 + x3) * 0.25F - x;
        float dy = (y0 + y1 + y2 + y3) * 0.25F - y;
        float dz = (z0 + z1 + z2 + z3) * 0.25F - z;
        
        return dx * dx + dy * dy + dz * dz;
    }
}
