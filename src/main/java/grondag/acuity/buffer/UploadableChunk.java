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

import java.nio.IntBuffer;

import grondag.acuity.api.pipeline.RenderPipelineImpl;
import grondag.acuity.buffer.VertexPackingList.VertexPackingConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class UploadableChunk<V extends DrawableChunk>
{
    protected final VertexPackingList packingList;
    protected final ObjectArrayList<DrawableChunkDelegate> delegates = DelegateLists.getReadyDelegateList();
    
    private static class UploadConsumer implements VertexPackingConsumer
    {
        ObjectArrayList<DrawableChunkDelegate> delegates;
        VertexCollectorList collectorList;
        int intOffset = 0;
        
        void prepare(ObjectArrayList<DrawableChunkDelegate> delegates, VertexCollectorList collectorList)
        {
            this.delegates = delegates;
            this.collectorList = collectorList;
        }
        
        @Override
        public void accept(RenderPipelineImpl pipeline, int vertexCount)
        {
            final int stride = pipeline.textureDepth.vertexFormat.stride;
            // array offset will be zero unless multiple buffers are needed
            intOffset = 0;
            MappedBufferManager.claimAllocation(pipeline, vertexCount * stride, ref ->
            {
                final int byteOffset = ref.byteOffset();
                final int byteCount = ref.byteCount();
                final int intLength = byteCount / 4;
                
                ref.lockForUpload();
                final IntBuffer intBuffer = ref.intBuffer();
                intBuffer.position(byteOffset / 4);
                intBuffer.put(collectorList.getIfExists(pipeline.getIndex()).rawData(), intOffset, intLength);
                ref.unlockForUpload();
                
                intOffset += intLength;
                final DrawableChunkDelegate delegate = new DrawableChunkDelegate(ref, pipeline, byteCount / stride);
                delegates.add(delegate);
            });            
        }
    }
    
    ThreadLocal<UploadConsumer> uploadConsumer = new ThreadLocal<UploadConsumer>()
    {
        @Override
        protected UploadConsumer initialValue()
        {
            return new UploadConsumer();
        }
    };
    
    protected UploadableChunk(VertexPackingList packingList, VertexCollectorList collectorList)
    {
        this.packingList = packingList;
        UploadConsumer uc = uploadConsumer.get();
        uc.prepare(delegates, collectorList);
        packingList.forEach(uc);
    }
    
    @Override
    protected void finalize()
    {
        assert this.delegates != null;
        
    }
    
    /**
     * Will be called from client thread - is where flush/unmap needs to happen.
     */
    public abstract V produceDrawable();
    
    /**
     * Called if {@link #produceDrawable()} will not be called, 
     * so can release MappedBuffer(s).
     */
    public final void cancel()
    {
        final int limit = delegates.size();
        for(int i = 0; i < limit; i++)
            delegates.get(i).release();
        
        delegates.clear();
    }
    
    public static class Solid extends UploadableChunk<DrawableChunk.Solid>
    {
        public Solid(VertexPackingList packing, VertexCollectorList collectorList)
        {
            super(packing, collectorList);
        }

        @Override
        public DrawableChunk.Solid produceDrawable()
        {
            final int limit = delegates.size();
            for(int i = 0; i < limit; i++)
                delegates.get(i).flush();
            return new DrawableChunk.Solid(delegates);
        }
    }

    public static class Translucent extends UploadableChunk<DrawableChunk.Translucent>
    {
        public Translucent(VertexPackingList packing, VertexCollectorList collectorList)
        {
            super(packing, collectorList);
        }

        @Override
        public DrawableChunk.Translucent produceDrawable()
        {
            final int limit = delegates.size();
            for(int i = 0; i < limit; i++)
                delegates.get(i).flush();
            return new DrawableChunk.Translucent(delegates);
        }
    }
}
