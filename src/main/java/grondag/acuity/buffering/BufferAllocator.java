package grondag.acuity.buffering;

import java.util.Arrays;
import java.util.Comparator;

import javax.annotation.Nullable;

import grondag.acuity.api.TextureFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@SuppressWarnings("null")
public class BufferAllocator
{
    // triangular number gives number of possible 1 or 2 slice allocation combinations
    public static final int COUNT = BufferSlice.SLICE_COUNT * (BufferSlice.SLICE_COUNT + 1) / 2;
    
    private static final BufferAllocator[][] ALLOCATORS;
    
    private static final Comparator<BufferAllocator> SIZE_SORTER = new Comparator<BufferAllocator>()
    {
        @Override
        public int compare(BufferAllocator o1, BufferAllocator o2)
        {
            return Integer.compare(o1.quadCount, o2.quadCount);
        }
    };
    
    static
    {
        ALLOCATORS = new BufferAllocator[TextureFormat.values().length][COUNT];
        initialize();
    }
    
    public static void initialize()
    {
        for(TextureFormat format : TextureFormat.values())
            initAllocators(format);
    }
    
    private static void initAllocators(TextureFormat format)
    {
        ObjectArrayList<BufferAllocator> builder = new ObjectArrayList<>();
        
        BufferSlice slice = BufferSlice.getSlice(format, BufferSlice.SLICE_COUNT - 1);
        
        do
        {
            builder.add(new BufferAllocator(slice, null));
            
            BufferSlice innerSlice = slice.bigger();
            while(innerSlice != null)
            {
                builder.add(new BufferAllocator(innerSlice, slice));
                innerSlice = innerSlice.bigger();
            }
            
            slice = slice.bigger();
        } 
        while(slice != null);
        
        BufferAllocator[] result = builder.toArray(new BufferAllocator[COUNT]);
        Arrays.sort(result, SIZE_SORTER);
        ALLOCATORS[format.ordinal()] = result;
    }
    
    public static BufferAllocator findBest(TextureFormat format, int forQuadCount)
    {
        final BufferAllocator[] allocators = ALLOCATORS[format.ordinal()];
        
        int low = 0;
        int high = COUNT; 
        while (low != high)
        {
            int mid = (low + high) / 2;
            if (allocators[mid].quadCount < forQuadCount)
                // midpoint is too small, therefore the one above it must be the new low
                low = mid + 1;
            else
                // mid is big enough, but may be too big - becomes the new high
                high = mid;
        }
        BufferAllocator result = allocators[low];
        assert result.quadCount >= forQuadCount;
        return result;
    }
    
    public final int bytes;
    public final int quadCount;
    public final BufferSlice primarySlice;
    public final @Nullable BufferSlice secondarySlice;
    public final boolean isDouble;
    public final int quadStride;
    
    private BufferAllocator(BufferSlice primarySlice, @Nullable BufferSlice secondarySlice)
    {
        this.primarySlice = primarySlice;
        this.secondarySlice = secondarySlice;
        this.isDouble = secondarySlice != null;
        this.bytes = primarySlice.bytes + (isDouble ? secondarySlice.bytes : 0);
        this.quadCount = primarySlice.quadCount +  (isDouble ? secondarySlice.quadCount : 0);
        this.quadStride = primarySlice.quadStride;
        
        assert this.bytes >= this.quadCount * this.quadStride;
    }
}
