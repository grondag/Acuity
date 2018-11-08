package grondag.acuity.buffering;

import java.nio.IntBuffer;

public interface IMappedBufferReference
{
    int byteCount();
    int byteOffset();
    int glBufferId();
    IntBuffer intBuffer();
    boolean isDisposed();
    void bind();
    void release();
    void flush();
}
