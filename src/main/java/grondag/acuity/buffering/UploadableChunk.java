package grondag.acuity.buffering;

import java.nio.IntBuffer;

import javax.annotation.Nullable;

import grondag.acuity.core.VertexCollectorList;
import grondag.acuity.core.VertexPackingList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class UploadableChunk<V extends DrawableChunk>
{
    protected final VertexPackingList packingList;
    protected ObjectArrayList<DrawableChunkDelegate> delegates = new ObjectArrayList<>();
    protected int intOffset = 0;
    
    protected UploadableChunk(VertexPackingList packingList, VertexCollectorList collectorList)
    {
        this.packingList = packingList;
        packingList.forEach((pipeline, vertexCount) ->
        {
            final int stride = pipeline.piplineVertexFormat().stride;
            MappedBufferStore.claimAllocation(pipeline, vertexCount * stride, ref ->
            {
                final int byteOffset = ref.byteOffset();
                final int byteCount = ref.byteCount();
                final int intLength = byteCount / 4;
                final IntBuffer intBuffer = ref.intBuffer();

                intBuffer.position(byteOffset / 4);
                intBuffer.put(collectorList.getIfExists(pipeline.getIndex()).rawData(), intOffset, intLength);
                intOffset += intLength;
                final DrawableChunkDelegate delegate = new DrawableChunkDelegate(ref, pipeline, byteCount / stride);
                delegates.add(delegate);
            });
        });
    }
    
    /**
     * Will be called from client thread - is where flush/unmap needs to happen.
     */
    public abstract @Nullable V produceDrawable();
    
    /**
     * Called if {@link #produceDrawable()} will not be called, 
     * so can release MappedBuffer(s).
     */
    public final void cancel()
    {
        delegates.forEach(d -> d.release());
        delegates.clear();
    }
    
    public static class Solid extends UploadableChunk<DrawableChunk.Solid>
    {
        public Solid(VertexPackingList packing, VertexCollectorList collectorList)
        {
            super(packing, collectorList);
        }

        @Override
        public @Nullable DrawableChunk.Solid produceDrawable()
        {
            delegates.forEach(d -> d.flush());
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
        public @Nullable DrawableChunk.Translucent produceDrawable()
        {
            delegates.forEach(d -> d.flush());
            return new DrawableChunk.Translucent(delegates);
        }
    }
}
