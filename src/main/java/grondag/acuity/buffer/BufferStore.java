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

package grondag.acuity.buffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import com.mojang.blaze3d.platform.GLX;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

/**
 * Holds a thread-safe cache of byte buffers to be used for VBO uploads.<br>
 * Currently not used.
 * 
 * TODO: remove if sticking with mem-mapped buffers
 */
@Environment(EnvType.CLIENT)
@Deprecated
public class BufferStore
{
    private static final ArrayBlockingQueue<ExpandableByteBuffer> store = new ArrayBlockingQueue<ExpandableByteBuffer>(4096);
    private static final int BUFFER_SIZE_INCREMENT = 0x200000;
    
    public static class ExpandableByteBuffer
    {
        private ByteBuffer byteBuffer;
        private IntBuffer intBuffer;
        
        private ExpandableByteBuffer()
        {
            byteBuffer = GLX.allocateMemory(BUFFER_SIZE_INCREMENT);
            intBuffer = byteBuffer.asIntBuffer();
        }
        
        public ByteBuffer byteBuffer()
        {
            return byteBuffer;
        }
        
        public IntBuffer intBuffer()
        {
            return intBuffer;
        }
        
        public void expand(int minByteSize)
        {
            if (minByteSize > this.byteBuffer.capacity())
            {
                ByteBuffer oldBuffer = this.byteBuffer;
                ByteBuffer newBuffer = GLX.allocateMemory(MathHelper.roundUp(minByteSize, BUFFER_SIZE_INCREMENT));
                int oldIntPos = this.intBuffer.position();
                int oldBytePos = this.byteBuffer.position();
                this.byteBuffer.position(0);
                newBuffer.put(this.byteBuffer);
                newBuffer.rewind();
                this.byteBuffer = newBuffer;
                this.intBuffer = newBuffer.asIntBuffer();
                newBuffer.position(oldBytePos);
                this.intBuffer.position(oldIntPos);
                
                GLX.freeMemory(oldBuffer);
            }
        }
       
    }
    
    public static ExpandableByteBuffer claim()
    {
        ExpandableByteBuffer result =  store.poll();
        return result == null ? new ExpandableByteBuffer() : result;
    }
    
    public static void release(ExpandableByteBuffer buffer)
    {
        buffer.byteBuffer.clear();
        store.offer(buffer);
    }
}
