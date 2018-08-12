package grondag.acuity.core;

import javax.annotation.Nullable;

import com.google.common.primitives.Floats;

import grondag.acuity.api.RenderPipeline;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VertexCollector
{
    private int[] data;
    private int integerSize = 0;
    private RenderPipeline pipeline;
    
    /**
     * Holds per-quad distance after {@link #sortQuads(float, float, float)} is called
     */
    @Nullable private float[] perQuadDistance;
    
    /**
     * Pointer to next sorted quad in sort iteration methods.<br>
     * After {@link #sortQuads(float, float, float)} is called this will be zero.
     */
    private int sortReadIndex = 0;
    
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
    
    public RenderPipeline pipeline()
    {
        return this.pipeline;
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
    
    private final void checkForSize(int toBeAdded)
    {
        if ((integerSize + toBeAdded) > data.length)
        {
            final int copy[] = new int[integerSize * 2];
            System.arraycopy(data, 0, copy, 0, integerSize);
            data  = copy;
        }
    }
    
    public final void add(final int i)
    {
        data[integerSize++] = i;
    }
    
    public final void add(final float f)
    {
        this.add(Float.floatToRawIntBits(f));
    }
    
    public final void pos(final BlockPos pos, float modelX, float modelY, float modelZ)
    {
        this.checkForSize(this.pipeline.piplineVertexFormat().stride);
        this.add(Utility.renderCubeRelative(pos.getX()) + modelX);
        this.add(Utility.renderCubeRelative(pos.getY()) + modelY);
        this.add(Utility.renderCubeRelative(pos.getZ()) + modelZ);
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

        // sort the indexes by distance - farthest first
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
        
        this.perQuadDistance = perQuadDistance;
        this.sortReadIndex = 0;
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
    
    @SuppressWarnings("null")
    public boolean hasUnpackedSortedQuads()
    {
        return this.perQuadDistance != null && this.sortReadIndex < this.perQuadDistance.length;
    }
    
    /**
     * Will return {@link Float#MIN_VALUE} if no unpacked quads remaining.
     */
    @SuppressWarnings("null")
    public float firstUnpackedDistance()
    {
        return hasUnpackedSortedQuads() ? this.perQuadDistance[this.sortReadIndex] : Float.MIN_VALUE;
    }
    
    /**
     * Returns the number of quads that are more or as distant than the distance provided
     * and advances the usage pointer so that {@link #firstUnpackedDistance()}
     * will return the distance to the next quad after that. <p>
     * 
     * (All distances are actually squared distances, to be clear.)
     */
    @SuppressWarnings("null")
    public int unpackUntilDistance(float minDistanceSquared)
    {
        if(!hasUnpackedSortedQuads())
            return 0;
        
        int result = 0;
        while(sortReadIndex < perQuadDistance.length && minDistanceSquared <= perQuadDistance[sortReadIndex])
        {
            result++;
            sortReadIndex++;
        }
        return result;
    }
}
