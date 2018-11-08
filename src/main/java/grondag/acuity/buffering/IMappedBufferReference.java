package grondag.acuity.buffering;

public interface IMappedBufferReference
{
    int byteCount();
    int byteOffset();
    MappedBuffer buffer();
}
