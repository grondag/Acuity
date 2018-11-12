package grondag.acuity.buffering;

import java.nio.IntBuffer;

//TODO: consolidate w/ implementation
public interface IMappedBufferDelegate
{
    int byteCount();
    int byteOffset();
    int glBufferId();
    IntBuffer intBuffer();
    boolean isDisposed();
    void bind();
    void flush();
    void release(DrawableChunkDelegate drawableChunkDelegate);
    void retain(DrawableChunkDelegate drawableChunkDelegate);
}
