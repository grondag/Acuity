package grondag.acuity.core;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.BufferStore.ExpandableByteBuffer;
import net.minecraft.util.math.MathHelper;

public class VertexCollectorList
{
    /**
     * Cache instantiated buffers for reuse.<p>
     */
    private static final ConcurrentLinkedQueue<VertexCollectorList> lists = new ConcurrentLinkedQueue<>();

    public static VertexCollectorList claim()
    {
        VertexCollectorList result = lists.poll();
        if(result == null)
            result = new VertexCollectorList();
        return result;
    }

    public static void release(VertexCollectorList list)
    {
        list.clear();
        lists.offer(list);
    }

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

    /**
     * Fast lookup of buffers by pipeline index. Null in CUTOUT layer buffers.
     */
    private VertexCollector[] vertexCollectors  = new VertexCollector[PipelineManager.MAX_PIPELINES];

    private int maxIndex = -1;

    /** used in transparency layer sorting - updated with player eye coordinates */
    private float viewX;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private float viewY;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private float viewZ;

    /** used in transparency layer sorting - updated with origin of render cube */
    private float renderOriginX = Float.NEGATIVE_INFINITY;
    /** used in transparency layer sorting - updated with origin of render cube */
    private float renderOriginY = Float.NEGATIVE_INFINITY;
    /** used in transparency layer sorting - updated with origin of render cube */
    private float renderOriginZ = Float.NEGATIVE_INFINITY;

    private VertexCollectorList()
    {
        //
    }

    /**
     * Releases any held vertex collectors and resets state
     */
    private void clear()
    {
        renderOriginX = Float.NEGATIVE_INFINITY;
        renderOriginY = Float.NEGATIVE_INFINITY;
        renderOriginZ = Float.NEGATIVE_INFINITY;
        
        final int limit = maxIndex;
        if(limit == -1) 
            return;

        maxIndex = -1;
        for(int i = 0; i <= limit; i++)
        {
            VertexCollector vc = vertexCollectors[i];
            if(vc == null)
                continue;
            VertexCollector.release(vc);
            vertexCollectors[i] = null;
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        clear();
    }

    public final boolean isEmpty()
    {
        return this.maxIndex == -1;
    }

    /**
     * Saves player eye coordinates for vertex sorting.
     * Normally these will be the same values for every list but
     * save in instance to stay consistent with Vanilla and possibly to 
     * support mods that do strange things with view entity perspective. (PortalGun?) 
     */
    public void setViewCoordinates(float x, float y, float z)
    {
        viewX = x;
        viewY = y;
        viewZ = z;
    }

    /**
     * Called by child collectors the first time they get a vertex.
     * All collectors in the list should share the same render origin.
     */
    public void setRenderOrigin(float x, float y, float z)
    {
        final float rX = RenderCube.renderCubeOrigin(MathHelper.fastFloor(x));
        final float rY = RenderCube.renderCubeOrigin(MathHelper.fastFloor(y));
        final float rZ = RenderCube.renderCubeOrigin(MathHelper.fastFloor(z));
        
        if(renderOriginX == Float.NEGATIVE_INFINITY)
        {
            renderOriginX = rX;
            renderOriginY = rY;
            renderOriginZ = rZ;
        }
        else
        {
            assert renderOriginX == rX;
            assert renderOriginY == rY;
            assert renderOriginZ == rZ;
        }
    }

    public final VertexCollector getIfExists(final int pipelineIndex)
    {
        return vertexCollectors[pipelineIndex];
    }

    public final VertexCollector getIfExists(RenderPipeline pipeline)
    {
        return vertexCollectors[pipeline.getIndex()];
    }

    public final VertexCollector getOrCreate(RenderPipeline pipeline)
    {
        final int i = pipeline.getIndex();
        VertexCollector result = vertexCollectors[i];
        if(result == null)
        {
            if(i > maxIndex)
                maxIndex = i;

            result = VertexCollector.claimAndPrepare(pipeline);
            result.parent = this;
            vertexCollectors[i] = result;
        }
        return result;
    }

    public final void forEachExisting(Consumer<VertexCollector> consumer)
    {
        final int limit = maxIndex;
        if(limit == -1) 
            return;

        for(int i = 0; i <= limit; i++)
        {
            VertexCollector vc = vertexCollectors[i];
            if(vc == null)
                continue;
            consumer.accept(vc);
        }
    }

    public final @Nullable Pair<ExpandableByteBuffer, VertexPackingList> packUpload()
    {
        VertexPackingList packing = new VertexPackingList();

        // NB: for solid render, relying on pipelines being added to packing in numerical order so that
        // all chunks can iterate pipelines independently while maintaining same pipeline order within chunk
        forEachExisting(vertexCollector ->
        {
            final int vertexCount = vertexCollector.vertexCount();
            if(vertexCount != 0)
                packing.addPacking(vertexCollector.pipeline(), vertexCount);
        });

        return packUpload(packing, this);
    }

    public final @Nullable Pair<ExpandableByteBuffer, VertexPackingList> packUploadSorted()
    {
        final VertexPackingList packing = new VertexPackingList();

        final PriorityQueue<VertexCollector> sorter = sorters.get();

        final float x = viewX - renderOriginX;
        final float y = viewY - renderOriginY;
        final float z = viewZ - renderOriginZ;
        
        // Sort quads within each pipeline, while accumulating in priority queue
        forEachExisting(vertexCollector ->
        {
            if(vertexCollector.vertexCount() != 0)
            {            
                vertexCollector.sortQuads(x, y, z);
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

        return packUpload(packing, this);
    }
}
