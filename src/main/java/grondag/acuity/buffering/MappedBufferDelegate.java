/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.buffering;

import java.nio.IntBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MappedBufferDelegate
{
    private final int byteCount;
    private final int byteOffset;
    private final MappedBuffer buffer;
    
    public MappedBufferDelegate(MappedBuffer buffer, int byteOffset, int byteCount)
    {
        this.buffer = buffer;
        this.byteCount = byteCount;
        this.byteOffset = byteOffset;
    }

    public final int byteCount()
    {
        return this.byteCount;
    }

    public final int byteOffset()
    {
        return this.byteOffset;
    }

    public final int glBufferId()
    {
        return buffer.glBufferId;
    }

    public final IntBuffer intBuffer()
    {
        return buffer.byteBuffer().asIntBuffer();
    }

    public final boolean isDisposed()
    {
        return buffer.isDisposed();
    }

    public final void bind()
    {
        buffer.bind();
    }

    public final void flush()
    {
        buffer.flush();
    }

    public final void release(DrawableChunkDelegate drawableChunkDelegate)
    {
        buffer.release(drawableChunkDelegate);
    }

    public final void retain(DrawableChunkDelegate drawableChunkDelegate)
    {
        buffer.retain(drawableChunkDelegate);
    }
    
    public final void lockForUpload()
    {
//        buffer.bufferLock.lock();
    }
    
    public final void unlockForUpload()
    {
//        buffer.bufferLock.unlock();
    }
}
