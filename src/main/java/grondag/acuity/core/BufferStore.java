package grondag.acuity.core;

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
