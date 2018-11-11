package grondag.acuity.buffering;

import java.nio.IntBuffer;

import grondag.acuity.api.TextureFormat;

public interface IBufferAllocation
{
    
    public int startVertex();
    public int quadCount();
    public boolean claim();
    public void release();
    public void setQuadCount(int quadCount);
    public BufferSlice slice();
    public MappedBuffer buffer();
    
    public default int glBufferId()
    {
        return buffer().glBufferId;
    }

    public default IntBuffer intBuffer()
    {
        return buffer().byteBuffer().asIntBuffer();
    }

    public default boolean isDisposed()
    {
        return buffer().isDisposed();
    }

    public default void bindForRender()
    {
        final MappedBuffer buffer = buffer();
        buffer.bind();
        buffer.bindVertexAttributes();
    }
    
    //TODO: need to flush allocation each and then flush buffer?
    public default void flush()
    {
        buffer().flush();
    }
    
    public default TextureFormat format()
    {
        return slice().format;
    }
    
    public default int quadStride()
    {
        return slice().quadStride;
    }
    
    public default boolean isMin()
    {
        return slice().isMin;
    }
    
    public default boolean isMax()
    {
        return slice().isMax;
    }
    
    public default int byteOffset()
    {
        return this.quadStride() * this.startVertex() / 4;
    }
    
    public default int byteCount()
    {
        return this.quadStride() * this.quadCount();
    }
    
    public default void flushLater()
    {
        this.buffer().flushLater(this);
    }
}
