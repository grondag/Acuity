package grondag.acuity.buffering;

import javax.annotation.Nullable;

import grondag.acuity.Configurator;
import grondag.acuity.api.TextureFormat;

public class BufferSlice
{
    /** Must be a power of 2 */
    public static final int MAX_BUFFER_BYTES = 0x400000;
    
    /** Must be a power of 2 */
    public static final int MIN_BUFFER_BYTES = 4096;

    public static final int SLICE_COUNT;
    
    private static final BufferSlice[] BIGGEST_SLICE;
    private static final BufferSlice[] SMALLEST_SLICE;

    private static final BufferSlice[][] SLICES;
    
    static
    {
        final int size = Integer.numberOfTrailingZeros(MAX_BUFFER_BYTES)
                - Integer.numberOfTrailingZeros(MIN_BUFFER_BYTES)
                + 1;
        
        SLICE_COUNT = size;
        BIGGEST_SLICE = new BufferSlice[TextureFormat.values().length];
        SMALLEST_SLICE = new BufferSlice[TextureFormat.values().length];
        SLICES = new BufferSlice[TextureFormat.values().length][SLICE_COUNT];
        
        initialize();
    }
    
    public static void initialize()
    {
        for(TextureFormat format : TextureFormat.values())
            initSlices(format);
    }
    
    private static void initSlices(TextureFormat format)
    {
        final int formatOrdinal = format.ordinal();
        BufferSlice[] slices = SLICES[format.ordinal()];
        final int quadStride = Configurator.lightingModel.vertexFormat(format).stride * 4;
        
        for(int i = 0; i < SLICE_COUNT; i++)
            slices[i] = new BufferSlice(i, quadStride, MAX_BUFFER_BYTES >> i, format);
        
        BIGGEST_SLICE[formatOrdinal] = slices[0];
        SMALLEST_SLICE[formatOrdinal] = slices[SLICE_COUNT - 1];
    }
    
    public static BufferSlice getSlice(TextureFormat format, int divisionLevel)
    {
        return SLICES[format.ordinal()][divisionLevel];
    }
    
    public final int divisionLevel;
    public final boolean isMax;
    public final boolean isMin;
    public final int quadCount;
    public final int quadStride;
    public final int bytes;
    public final TextureFormat format;
    public final int formatOrdinal;
    
    private BufferSlice(int divisionLevel, int quadStride, int bytes, TextureFormat format)
    {
        this.divisionLevel = divisionLevel;
        this.quadStride = quadStride;
        this.bytes = bytes;
        this.quadCount = bytes / quadStride;
        this.isMax = divisionLevel == 0;
        this.isMin = divisionLevel == SLICE_COUNT - 1;
        this.format = format;
        this.formatOrdinal = format.ordinal();
    }
    
    public final @Nullable BufferSlice bigger()
    {
        return this.isMax ? null : SLICES[formatOrdinal][divisionLevel - 1];
    }
    
    public final @Nullable BufferSlice smaller()
    {
        return this.isMin ? null : SLICES[formatOrdinal][divisionLevel + 1];
    }
}
