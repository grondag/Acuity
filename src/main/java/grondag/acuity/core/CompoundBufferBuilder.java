package grondag.acuity.core;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;

import javax.annotation.Nullable;

import grondag.acuity.Acuity;
import grondag.acuity.api.IPipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.BufferStore.ExpandableByteBuffer;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundBufferBuilder extends BufferBuilder
{
    private static final VertexCollector[] EMPTY_ARRAY = new VertexCollector[IPipelineManager.MAX_PIPELINES];
    
    /**
     * Cache instantiated buffers for reuse.<p>
     */
    private final ObjectArrayFIFOQueue<VertexCollector> collectors = new ObjectArrayFIFOQueue<>();
    
    /**
     * Track pipelines in use as list for fast upload 
     * and to know if we ned to allocate more.  Never includes the vanilla pipeline.
     */
    private final ObjectArrayList<RenderPipeline> pipelineList = new ObjectArrayList<>();
    
    /**
     * Fast lookup of buffers by pipeline index.  Element 0 will always be this.
     */
    private final VertexCollector[] pipelineArray = new VertexCollector[IPipelineManager.MAX_PIPELINES];
    
    /**
     * Holds vertex data ready for upload if we have it.
     * Obtained from BufferStore and released back to store when uploads are complete.
     */
    @Nullable ExpandableByteBuffer uploadBuffer  = null;
    
    /**
     * Describes vertex packing in {@link #uploadBuffer}.
     * Must be a new instance each time because will be
     * passed on to rendering to control vertex draw batches.
     */
    @Nullable VertexPackingList uploadPackingList = null;
    
    private class CompoundState extends State
    {
        private ObjectArrayList<RenderPipeline> pipelineList;
        private VertexCollector[] pipelineArray = new VertexCollector[IPipelineManager.MAX_PIPELINES];
        
        @SuppressWarnings("null")
        public CompoundState(int[] buffer, VertexFormat format)
        {
            super(buffer, format);
        }
    }
    
    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(limitBufferSize(bufferSizeIn));
    }
    
    // the RegionRenderCacheBuilder instantiates this with pretty large sizes
    // but in most cases the super instance won't be used when the mod is enabled
    // so don't honor these when mod is enabled to reduce memory footprint
    static final int limitBufferSize(int bufferSizeIn)
    {
        if(Acuity.isModEnabled())
        {
            if(bufferSizeIn == 2097152 || bufferSizeIn == 131072 || bufferSizeIn == 262144)
            {
                return 2800;
            }
        }
        return bufferSizeIn;
    }
    
    @Override
    public State getVertexState()
    {
        if(Acuity.isModEnabled())
        {
            State inner = super.getVertexState();
            CompoundState result = new CompoundState(inner.getRawBuffer(), inner.getVertexFormat());
            result.pipelineList = this.pipelineList.clone();
            System.arraycopy(this.pipelineArray, 0, result.pipelineArray, 0, IPipelineManager.MAX_PIPELINES);
            return result;
        }
        else
            return super.getVertexState();
    }

    @Override
    public void setVertexState(State state)
    {
        super.setVertexState(state);
        if(Acuity.isModEnabled())
        {
            CompoundState compState = (CompoundState)state;
            this.pipelineList.clear();
            this.pipelineList.addAll(compState.pipelineList);
            System.arraycopy(compState.pipelineArray, 0, this.pipelineArray, 0, this.pipelineArray.length);
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        if(Acuity.isModEnabled())
        {
            System.arraycopy(EMPTY_ARRAY, 0, pipelineArray, 0, IPipelineManager.MAX_PIPELINES);
            this.pipelineList.clear();
            this.uploadBuffer = null;
            this.uploadPackingList = null;
        }
    }
    
    public VertexCollector getVertexCollector(RenderPipeline pipeline)
    {
        final int i = pipeline.getIndex();
        VertexCollector result = pipelineArray[i];
        if(result == null)
        {
            result = getInitializedCollector(pipeline);
            pipelineArray[i] = result;
            pipelineList.add(pipeline);
        }
        return result;
    }
    
    private VertexCollector getInitializedCollector(RenderPipeline pipeline)
    {
        VertexCollector result = this.collectors.isEmpty() ? new VertexCollector(1024) : this.collectors.dequeue();
        result.prepare(pipeline);
        return result;
    }

    @Override
    public void finishDrawing()
    {
        super.finishDrawing();
        
        if(Acuity.isModEnabled())
        {
            // In transparency layer, packing list will already
            // have been built via vertex sort.  
            // If it is null, assume is non-transparent layer and build it now.
            VertexPackingList packing = this.uploadPackingList;
            if(packing == null)
            {
                packing = new VertexPackingList();
                if(!pipelineList.isEmpty())
                {
                    for(RenderPipeline p : pipelineList)
                    {
                        final VertexCollector b = pipelineArray[p.getIndex()];
                        final int vertexCount = b.vertexCount();
                        if(vertexCount != 0)
                            packing.addPacking(p, vertexCount);
                        
                        // Collectors used in non-transparency layers can be reused.
                        // (Transparency collectors are retained in compiled chunk for resorting.)
                        this.collectors.enqueue(b);
                    }
                }
                this.uploadPackingList = packing;
            }
            this.prepareUploadBuffer();
        }
    }

    private void prepareUploadBuffer()
    {
        final VertexPackingList packing = this.uploadPackingList;
        if(packing == null)
            return;
        
        // tracks current position within vertex collectors
        // necessary in transparency layer when splitting pipelines
        int[] pipelineStarts = new int[IPipelineManager.MAX_PIPELINES];
        
        final ExpandableByteBuffer buffer = BufferStore.claim();
        buffer.expand(packing.totalBytes());
        final IntBuffer intBuffer = buffer.intBuffer();
        intBuffer.position(0);
        
        packing.forEach((RenderPipeline p, int vertexCount) ->
        {
            final int pipelineIndex = p.getIndex();
            final int startInt = pipelineStarts[pipelineIndex];
            final int intLength = vertexCount * p.piplineVertexFormat().stride / 4;
            intBuffer.put(pipelineArray[pipelineIndex].rawData(), startInt, intLength);
            pipelineStarts[pipelineIndex] = startInt + intLength;
        });
        buffer.byteBuffer().limit(packing.totalBytes());
        this.uploadBuffer = buffer;
    }
    
    public void uploadTo(CompoundVertexBuffer target)
    {   
        final ExpandableByteBuffer uploadBuffer = this.uploadBuffer;
        final VertexPackingList packingList = this.uploadPackingList;
        if(uploadBuffer != null)
        {
            if(packingList != null)
                target.upload(uploadBuffer.byteBuffer(), packingList);
            
            BufferStore.release(uploadBuffer);
        }
        this.uploadBuffer = null;
        this.uploadPackingList = null;
    }
    
    @Override
    public void sortVertexData(float x, float y, float z)
    {
        if(Acuity.isModEnabled())
            sortCompondVertexData(x, y, z);
        else
            super.sortVertexData(x, y, z);
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
    
    private void sortCompondVertexData(float x, float y, float z)
    {
        // First sort quads within each pipeline
        final float relativeX = (float)((double)x + this.xOffset);
        final float relativeY = (float)((double)y + this.yOffset);
        final float relativeZ = (float)((double)z + this.zOffset);
        
        VertexPackingList packing = new VertexPackingList();
        packing = new VertexPackingList();

        // Exploit special case when there is only one transparent pipeline in this renderchunk
        if(pipelineList.size() == 1)
        {
            RenderPipeline p = pipelineList.get(0);
            final VertexCollector collector = pipelineArray[p.getIndex()];
            collector.sortQuads(relativeX, relativeY, relativeZ);
            final int vertexCount = collector.vertexCount();
            if(vertexCount != 0)
                packing.addPacking(p, vertexCount);
        }
        else if(!pipelineList.isEmpty())
        {
            final PriorityQueue<VertexCollector> sorter = sorters.get();
            
            for(RenderPipeline p : this.pipelineList)
            {
                final VertexCollector collector = pipelineArray[p.getIndex()];
                collector.sortQuads(relativeX, relativeY, relativeZ);
                sorter.add(collector);
            }
            
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
        
        this.uploadPackingList = packing;
    }
}
