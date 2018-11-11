package grondag.acuity.core;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import com.google.common.primitives.Doubles;

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
    /**
     * Cache instantiated buffers for reuse.<p>
     */
    private static final ConcurrentLinkedQueue<VertexCollector> collectors = new ConcurrentLinkedQueue<>();

    public @Nullable VertexCollectorList parent; 
    
    public static VertexCollector claimAndPrepare(RenderPipeline pipeline)
    {
        VertexCollector result = collectors.poll();
        if(result == null)
            result = new VertexCollector(1024);
        result.prepare(pipeline);
        return result;
    }
    
    public static void release(VertexCollector collector)
    {
        collector.parent = null;
        collectors.offer(collector);
    }
    
    private int[] data;
    private int integerSize = 0;
    private RenderPipeline pipeline;
    
    /**
     * Holds per-quad distance after {@link #sortQuads(double, double, double)} is called
     */
    @Nullable private double[] perQuadDistance;
    
    /**
     * Pointer to next sorted quad in sort iteration methods.<br>
     * After {@link #sortQuads(float, float, float)} is called this will be zero.
     */
    private int sortReadIndex = 0;
    
    public VertexCollector()
    {
        this(128);
    }
    
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
        // assumes first addition will always be position
        if(integerSize == 0 && parent != null)
            parent.setRenderOrigin(pos.getX(), pos.getY(), pos.getZ());
            
        this.checkForSize(this.pipeline.piplineVertexFormat().stride);
        this.add(RenderCube.renderCubeRelative(pos.getX()) + modelX);
        this.add(RenderCube.renderCubeRelative(pos.getY()) + modelY);
        this.add(RenderCube.renderCubeRelative(pos.getZ()) + modelZ);
    }
    
    @SuppressWarnings("serial")
    public void sortQuads(double x, double y, double z)
    {
         // works because 4 bytes per int
        final int quadIntStride = this.pipeline.piplineVertexFormat().stride;
        final int vertexIntStride = quadIntStride / 4;
        final int quadCount = this.vertexCount() / 4;
        final double[] perQuadDistance = new double[quadCount];
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
                return Doubles.compare(perQuadDistance[b], perQuadDistance[a]);
            }
        },
        new Swapper()
        {
            @Override
            public void swap(int a, int b)
            {
                double distSwap = perQuadDistance[a];
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
    
    private double getDistanceSq(double x, double y, double z, int integerStride, int vertexIndex)
    {
        // unpack vertex coordinates
        int i = vertexIndex * integerStride * 4;
        double x0 = Float.intBitsToFloat(this.data[i]);
        double y0 = Float.intBitsToFloat(this.data[i + 1]);
        double z0 = Float.intBitsToFloat(this.data[i + 2]);
        
        i += integerStride;
        double x1 = Float.intBitsToFloat(this.data[i]);
        double y1 = Float.intBitsToFloat(this.data[i + 1]);
        double z1 = Float.intBitsToFloat(this.data[i + 2]);
        
        i += integerStride;
        double x2 = Float.intBitsToFloat(this.data[i]);
        double y2 = Float.intBitsToFloat(this.data[i + 1]);
        double z2 = Float.intBitsToFloat(this.data[i + 2]);
        
        i += integerStride;
        double x3 = Float.intBitsToFloat(this.data[i]);
        double y3 = Float.intBitsToFloat(this.data[i + 1]);
        double z3 = Float.intBitsToFloat(this.data[i + 2]);
        
        // compute average distance by component
        double dx = (x0 + x1 + x2 + x3) * 0.25 - x;
        double dy = (y0 + y1 + y2 + y3) * 0.25 - y;
        double dz = (z0 + z1 + z2 + z3) * 0.25 - z;
        
        return dx * dx + dy * dy + dz * dz;
    }
    
    @SuppressWarnings("null")
    public boolean hasUnpackedSortedQuads()
    {
        return this.perQuadDistance != null && this.sortReadIndex < this.perQuadDistance.length;
    }
    
    /**
     * Will return {@link Double#MIN_VALUE} if no unpacked quads remaining.
     */
    @SuppressWarnings("null")
    public double firstUnpackedDistance()
    {
        return hasUnpackedSortedQuads() ? this.perQuadDistance[this.sortReadIndex] : Double.MIN_VALUE;
    }
    
    /**
     * Returns the number of quads that are more or as distant than the distance provided
     * and advances the usage pointer so that {@link #firstUnpackedDistance()}
     * will return the distance to the next quad after that. <p>
     * 
     * (All distances are actually squared distances, to be clear.)
     */
    @SuppressWarnings("null")
    public int unpackUntilDistance(double minDistanceSquared)
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
