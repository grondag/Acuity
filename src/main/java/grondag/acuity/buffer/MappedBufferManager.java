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

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import grondag.acuity.api.pipeline.RenderPipelineImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;


//PERF: provide diff buffers by vertex format and handle VAO binding 1X per buffer bind in buffers
@Environment(EnvType.CLIENT)
public class MappedBufferManager
{
    private static final ConcurrentSkipListMap<Long, MappedBuffer> BUFFERS = new ConcurrentSkipListMap<>();
    
    private static final int KEY_SHIFT_BITS = 16;
    
    /**
     * If byteCount is larger than a single buffer will give consumer more than one buffer w/ 
     * offsets able to contain the given byte count. Otherwise will always call consumer 1X with
     * an allocation that contains the entire byte count.
     * If more than one buffer is needed, break(s) will be at a boundary compatible with all vertex formats.
     * All vertices in the buffer(s) will share the same pipeline (and thus vertex format).
     */
    public static void claimAllocation(RenderPipelineImpl pipeline, int byteCount, Consumer<MappedBufferDelegate> consumer)
    {
        while(byteCount >= MappedBuffer.CAPACITY_BYTES)
        {
                MappedBuffer target = MappedBufferStore.getEmptyMapped();
                if(target == null)
                    return;
                MappedBufferDelegate result = target.requestBytes(byteCount, pipeline.textureDepth.vertexFormat.stride * 4);
                assert result != null;
                if(result == null)
                    return;
                consumer.accept(result);
                byteCount -= result.byteCount();
                handleRemainder(target);
        }
        
        if(byteCount != 0)
            claimPartialAllocation(byteCount, consumer);
    }
    
    private static void claimPartialAllocation(final int byteCount, Consumer<MappedBufferDelegate> consumer)
    {
        final long byteKey = ((long)byteCount) << KEY_SHIFT_BITS;
        
        Long candidateKey = BUFFERS.ceilingKey(byteKey);
        
        while(candidateKey != null)
        {
            MappedBuffer target = BUFFERS.remove(candidateKey);
            if(target == null)
            {
                candidateKey = BUFFERS.ceilingKey(byteKey);
                continue;
            }
            
            MappedBufferDelegate result = target.requestBytes(byteCount, byteCount);
            assert result != null;
            if(result == null)
                return;
            assert result.byteCount() == byteCount;
      
            consumer.accept(result);
            handleRemainder(target);
            return;
        }
        
        // nothing available so get a new buffer
        MappedBuffer target = MappedBufferStore.getEmptyMapped();
        if(target == null)
            return;
        MappedBufferDelegate result = target.requestBytes(byteCount, byteCount);
        assert result != null;
        if(result == null)
            return;
        consumer.accept(result);
        handleRemainder(target);
    }

    private static void handleRemainder(MappedBuffer target)
    {
        final int remainingBytes = target.unallocatedBytes();
        if(remainingBytes < 4096)
            target.setFinal();
        else
        {
            final Long byteKey = (((long)remainingBytes) << KEY_SHIFT_BITS) | target.id;
            BUFFERS.put(byteKey, target);
        }
    }

    public static void forceReload()
    {
        BUFFERS.clear();
    }
}
