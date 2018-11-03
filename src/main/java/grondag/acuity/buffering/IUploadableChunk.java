package grondag.acuity.buffering;

import grondag.acuity.core.BufferStore.ExpandableByteBuffer;

import java.nio.IntBuffer;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.core.BufferStore;
import grondag.acuity.core.VertexCollectorList;
import grondag.acuity.core.VertexPackingList;

//TODO: remove when WIP done
public interface IUploadableChunk
{
    public static class Temporary implements IUploadableChunk
    {
        public final ExpandableByteBuffer byteBuffer;
        public final VertexPackingList packingList;
        
        public Temporary(final VertexPackingList packing, 
                final VertexCollectorList collectorList,
                final boolean isSolid)
        {
            // tracks current position within vertex collectors
            // necessary in transparency layer when splitting pipelines
            final int[] pipelineStarts = new int[PipelineManager.MAX_PIPELINES];

            final ExpandableByteBuffer buffer = BufferStore.claim();
            buffer.expand(packing.totalBytes());
            final IntBuffer intBuffer = buffer.intBuffer();
            intBuffer.position(0);

            packing.forEach((pipeline, vertexCount) ->
            {
                final int pipelineIndex = pipeline.getIndex();
                final int startInt = pipelineStarts[pipelineIndex];
                final int intLength = vertexCount * pipeline.piplineVertexFormat().stride / 4;
                intBuffer.put(collectorList.getIfExists(pipelineIndex).rawData(), startInt, intLength);
                pipelineStarts[pipelineIndex] = startInt + intLength;
            });

            buffer.byteBuffer().limit(packing.totalBytes());
            this.byteBuffer = buffer;
            this.packingList = packing;
        }
    }
}
