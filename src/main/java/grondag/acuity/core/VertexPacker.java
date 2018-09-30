package grondag.acuity.core;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.core.BufferStore.ExpandableByteBuffer;

public class VertexPacker
{
    /**
     * Will return null if packing is empty;
     */
    private static final @Nullable Pair<ExpandableByteBuffer, VertexPackingList> packUpload(final VertexPackingList packing, final VertexCollectorList collectorList)
    {
        if(packing.size() == 0)
            return null;
        
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
        return Pair.of(buffer, packing);
    }
    
    public static final @Nullable Pair<ExpandableByteBuffer, VertexPackingList> packUpload(final VertexCollectorList collectorList)
    {
        VertexPackingList packing = new VertexPackingList();
        
        // NB: for solid render, relying on pipelines being added to packing in numerical order so that
        // all chunks can iterate pipelines independently while maintaining same pipeline order within chunk
        collectorList.forEachExisting(vertexCollector ->
        {
            final int vertexCount = vertexCollector.vertexCount();
            if(vertexCount != 0)
                packing.addPacking(vertexCollector.pipeline(), vertexCount);
        });
        
        return packUpload(packing, collectorList);
    }
    
    private static final Comparator<VertexCollector> vertexCollectionComparator = new Comparator<VertexCollector>() 
    {
        @SuppressWarnings("null")
        @Override
        public int compare(VertexCollector o1, VertexCollector o2)
        {
            // note reverse order - take most distant first
            return Float.compare(o2.firstUnpackedDistance(), o1.firstUnpackedDistance());
        }
    };
    
    private static final ThreadLocal<PriorityQueue<VertexCollector>> sorters = new ThreadLocal<PriorityQueue<VertexCollector>>()
    {
        @Override
        protected PriorityQueue<VertexCollector> initialValue()
        {
            return new PriorityQueue<VertexCollector>(vertexCollectionComparator);
        }

        @Override
        public PriorityQueue<VertexCollector> get()
        {
            PriorityQueue<VertexCollector> result = super.get();
            result.clear();
            return result;
        }
    };
    
    public static final @Nullable Pair<ExpandableByteBuffer, VertexPackingList> packUploadSorted(final VertexCollectorList collectorList,
            final float relativeX, final float relativeY, final float relativeZ)
    {
        final VertexPackingList packing = new VertexPackingList();

        final PriorityQueue<VertexCollector> sorter = sorters.get();
        
        // Sort quads within each pipeline, while accumulating in priority queue
        collectorList.forEachExisting(vertexCollector ->
        {
            if(vertexCollector.vertexCount() != 0)
            {            
                vertexCollector.sortQuads(relativeX, relativeY, relativeZ);
                sorter.add(vertexCollector);
            }
        });
        
        // exploit special case when only one transparent pipeline in this render chunk
        if(sorter.size() == 1)
        {
            VertexCollector only = sorter.poll();
            packing.addPacking(only.pipeline(), only.vertexCount());
        }
        else if(sorter.size() != 0)
        {
            VertexCollector first = sorter.poll();
            VertexCollector second = sorter.poll();
            do
            {   
                // x4 because packing is vertices vs quads
                packing.addPacking(first.pipeline(), 4 * first.unpackUntilDistance(second.firstUnpackedDistance()));
                
                if(first.hasUnpackedSortedQuads())
                    sorter.add(first);
                
                first = second;
                second = sorter.poll();
                
            } while(second != null);
            
            packing.addPacking(first.pipeline(), 4 * first.unpackUntilDistance(Float.MIN_VALUE));
        }
        
        return packUpload(packing, collectorList);
    }
}
