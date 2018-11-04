package grondag.acuity.buffering;

import java.nio.IntBuffer;

import javax.annotation.Nullable;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.core.VertexCollectorList;
import grondag.acuity.core.VertexPackingList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class UploadableChunk<V extends DrawableChunk>
{
    protected final VertexPackingList packingList;
    
    protected UploadableChunk(VertexPackingList packingList)
    {
        this.packingList = packingList;
    }
    
    /**
     * Will be called from client thread - is where flush/unmap needs to happen.
     */
    public abstract @Nullable V produceDrawable();
    
    /**
     * Called if {@link #produceDrawable()} will not be called, 
     * so can release MappedBuffer(s).
     */
    public abstract void cancel();
    
    public static class Solid extends UploadableChunk<DrawableChunk.Solid>
    {
        ObjectArrayList<SolidDrawableChunkDelegate> delegates = new ObjectArrayList<>();
        int intOffset = 0;
        
        public Solid(VertexPackingList packing, VertexCollectorList collectorList)
        {
            super(packing);
            packing.forEach((pipeline, vertexCount) ->
            {
                final int stride = pipeline.piplineVertexFormat().stride;
                MappedBufferStore.claimSolid(pipeline, vertexCount * stride, (byteOffset, byteCount, buffer) ->
                {
                    final IntBuffer intBuffer = buffer.byteBuffer().asIntBuffer();
                    intBuffer.position(byteOffset / 4);
                    final int intLength = byteCount / 4;
                    intBuffer.put(collectorList.getIfExists(pipeline.getIndex()).rawData(), intOffset, intLength);
                    intOffset += intLength; 
                    delegates.add(new SolidDrawableChunkDelegate(buffer, pipeline, byteOffset / stride, byteCount / stride));
                });
            });
        }

        @Override
        public @Nullable DrawableChunk.Solid produceDrawable()
        {
            delegates.forEach(d -> d.flush());
            return new DrawableChunk.Solid(delegates);
        }

        @Override
        public void cancel()
        {
            delegates.forEach(d -> d.release());
            delegates.clear();
        }
    }

    public static class Translucent extends UploadableChunk<DrawableChunk.Translucent>
    {
        private final static int[] EMPTY_START = new int[PipelineManager.MAX_PIPELINES];
        private final static ThreadLocal<int[]> starters = new ThreadLocal<int[]>()
        {
            @Override
            protected int[] initialValue()
            {
                return new int[PipelineManager.MAX_PIPELINES];
            }
        };
        
        private @Nullable MappedBuffer mappedBuffer;
        private int bufferByteOffset;
        
        public Translucent(VertexPackingList packing, VertexCollectorList collectorList)
        {
            super(packing);
            // tracks current position within vertex collectors
            // necessary in transparency layer when splitting pipelines
            final int[] pipelineStarts = starters.get();
            System.arraycopy(EMPTY_START, 0, pipelineStarts, 0, PipelineManager.MAX_PIPELINES);
            
            assert packing.totalBytes() != 0;
            
            MappedBufferStore.claimTranslucent(packing.totalBytes(), (byteOffset, byteCount, buffer) ->
            {
                assert byteCount == packing.totalBytes();
                
                if(byteCount != 0)
                {
                    bufferByteOffset = byteOffset;
                    
                    final IntBuffer intBuffer = buffer.byteBuffer().asIntBuffer();
                    
                    intBuffer.position(byteOffset / 4);

                    packing.forEach((pipeline, vertexCount) ->
                    {
                        final int pipelineIndex = pipeline.getIndex();
                        final int startInt = pipelineStarts[pipelineIndex];
                        final int intLength = vertexCount * pipeline.piplineVertexFormat().stride / 4;
                        intBuffer.put(collectorList.getIfExists(pipelineIndex).rawData(), startInt, intLength);
                        pipelineStarts[pipelineIndex] = startInt + intLength;
                    });
                    mappedBuffer = buffer;
                }
            });
        }

        @SuppressWarnings("null")
        @Override
        public @Nullable DrawableChunk.Translucent produceDrawable()
        {
            if(mappedBuffer == null || packingList == null)
                return null;
            
            mappedBuffer.flush();
            return new DrawableChunk.Translucent(mappedBuffer, packingList, bufferByteOffset);
        }

        @Override
        public void cancel()
        {
            if(mappedBuffer != null)
            {
                mappedBuffer.release();
                mappedBuffer = null;
            }
        }
        
    }
}
