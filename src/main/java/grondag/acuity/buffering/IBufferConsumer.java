package grondag.acuity.buffering;

@FunctionalInterface
public interface IBufferConsumer
{
    public void accept(int byteOffset, int byteCount, MappedBuffer buffer);
}
