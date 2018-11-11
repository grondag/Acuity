package grondag.acuity.buffering;

import java.nio.IntBuffer;

public class SimpleMappedBufferReference implements IMappedBufferDelegate
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
    public int glBufferId()
    {
        return buffer.glBufferId;
    }

    @Override
    public IntBuffer intBuffer()
    {
        return buffer.byteBuffer().asIntBuffer();
    }

    @Override
    public boolean isDisposed()
    {
        return buffer.isDisposed();
    }

    @Override
    public void bind()
    {
        buffer.bind();
    }

    @Override
    public void flush()
    {
        buffer.flush();
    }

    @Override
    public void release(DrawableChunkDelegate drawableChunkDelegate)
    {
        buffer.release(drawableChunkDelegate);
    }

    @Override
    public void retain(DrawableChunkDelegate drawableChunkDelegate)
    {
        buffer.retain(drawableChunkDelegate);
    }
}
