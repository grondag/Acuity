package grondag.acuity.buffering;

public class SimpleMappedBufferReference implements IMappedBufferReference
{
    private final int byteCount;
    private final int byteOffset;
    private final MappedBuffer buffer;
    
    public SimpleMappedBufferReference(MappedBuffer buffer, int byteOffset, int byteCount)
    {
        this.buffer = buffer;
        this.byteCount = byteCount;
        this.byteOffset = byteOffset;
    }

    @Override
    public int byteCount()
    {
        return this.byteCount;
    }

    @Override
    public int byteOffset()
    {
        return this.byteOffset;
    }

    @Override
    public MappedBuffer buffer()
    {
        return this.buffer;
    }
}
