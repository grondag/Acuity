package grondag.acuity.core;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import grondag.acuity.Acuity;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.BufferStore.ExpandableByteBuffer;
import grondag.acuity.core.VertexPackingList.VertexPackingConsumer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundBufferBuilder extends BufferBuilder
{
    private static final VertexCollector[] EMPTY_ARRAY = new VertexCollector[PipelineManager.MAX_PIPELINES];
    
    /**
     * Cache instantiated buffers for reuse.<p>
     */
    private static final ConcurrentLinkedQueue<VertexCollector> collectors = new ConcurrentLinkedQueue<>();
    
    /**
     * Fast lookup of buffers by pipeline index. Null in CUTOUT layer buffers.
     */
    private VertexCollector[] pipelineArray;
    
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
    
    /**
     * Tells us which block layer we are buffering.
     * Can be used with {@link #owner} to find peer buffers for other layers.
     * Could also be handy for other purposes.
     */
    private @Nullable BlockRenderLayer layer;

    private @Nullable CompoundBufferBuilder proxy;
    
    private class CompoundState extends State
    {
        @SuppressWarnings("hiding")
        private VertexCollector[] pipelineArray = new VertexCollector[PipelineManager.MAX_PIPELINES];
        
        public CompoundState(int[] buffer, VertexFormat format)
        {
            super(buffer, format);
        }
    }
    
    @SuppressWarnings("null")
    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(limitBufferSize(bufferSizeIn));
    }
    
    /**
     * Called at end of RegionRenderCacheBuilder init via ASM.
     */
    @SuppressWarnings("null")
    public void setupLinks(RegionRenderCacheBuilder owner, BlockRenderLayer layer)
    {
        this.layer = layer;
        
        if(this.layer == BlockRenderLayer.CUTOUT || this.layer == BlockRenderLayer.CUTOUT_MIPPED)
        {
            this.pipelineArray = null;
            this.proxy = (CompoundBufferBuilder) owner.getWorldRendererByLayer(BlockRenderLayer.SOLID);
        }
        else
            this.pipelineArray = new VertexCollector[PipelineManager.MAX_PIPELINES];
    }
    
    /**
     * The RegionRenderCacheBuilder instantiates this with significant sizes
     * but in most cases the super instance won't be used when the mod is enabled
     * so don't honor these when mod is enabled to reduce memory footprint.
     */
    static final int limitBufferSize(int bufferSizeIn)
    {
        if(Acuity.isModEnabled())
        {
            if(bufferSizeIn == 2097152 || bufferSizeIn == 131072 || bufferSizeIn == 262144)
            {
                return 2048;
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
            System.arraycopy(this.pipelineArray, 0, result.pipelineArray, 0, PipelineManager.MAX_PIPELINES);
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
            System.arraycopy(compState.pipelineArray, 0, this.pipelineArray, 0, this.pipelineArray.length);
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        if(Acuity.isModEnabled())
        {
            System.arraycopy(EMPTY_ARRAY, 0, pipelineArray, 0, PipelineManager.MAX_PIPELINES);
            this.uploadBuffer = null;
            this.uploadPackingList = null;
        }
    }
    
    public VertexCollector getVertexCollector(RenderPipeline pipeline)
    {
        if(Acuity.isModEnabled() && this.proxy != null)
            return this.proxy.getVertexCollector(pipeline);
        
        final int i = pipeline.getIndex();
        VertexCollector result = pipelineArray[i];
        if(result == null)
        {
            result = getInitializedCollector(pipeline);
            pipelineArray[i] = result;
        }
        return result;
    }
    
    private VertexCollector getInitializedCollector(RenderPipeline pipeline)
    {
        VertexCollector result = collectors.poll();
        if(result == null)
            result = new VertexCollector(1024);
        result.prepare(pipeline);
        return result;
    }

    public void beginIfNotAlreadyDrawing(int glMode, VertexFormat format)
    {
        if(!this.isDrawing)
            super.begin(glMode, format);
    }
    
    @Override
    public void begin(int glMode, VertexFormat format)
    {
        if(Acuity.isModEnabled() && proxy != null)
            proxy.beginIfNotAlreadyDrawing(glMode, format);
        else
            beginIfNotAlreadyDrawing(glMode, format);
    }
    
    public void finishDrawingIfNotAlreadyFinished()
    {
        if(this.isDrawing)
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
                    
                    // NB: for solid render, relying on pipelines being added to packing in numerical order so that
                    // all chunks can iterate pipelines independently while maintaining same pipeline order within chunk
                    for(VertexCollector b : pipelineArray)
                    {
                        if(b != null)
                        {
                            final int vertexCount = b.vertexCount();
                            if(vertexCount != 0)
                                packing.addPacking(b.pipeline(), vertexCount);
                            
                            // Collectors used in non-transparency layers can be reused.
                            // (Transparency collectors are retained in compiled chunk for resorting.)
                            collectors.offer(b);
                        }
                    }
                    this.uploadPackingList = packing;
                }
                this.packingConsumer.packUpload();
            }
        }
    }
    
    @Override
    public void finishDrawing()
    {
        if(Acuity.isModEnabled() && proxy != null)
        {
            proxy.finishDrawingIfNotAlreadyFinished();
            return;
        }
        else
            this.finishDrawingIfNotAlreadyFinished();
       
    }

    private static final int[] EMPTY_STARTS = new int[PipelineManager.MAX_PIPELINES];
    
    private final class Consumer extends VertexPackingConsumer
    {
        // tracks current position within vertex collectors
        // necessary in transparency layer when splitting pipelines
        final int[] pipelineStarts = new int[PipelineManager.MAX_PIPELINES];
        
        @SuppressWarnings("null")
        IntBuffer intBuffer;
        
        private final void packUpload()
        {
            final VertexPackingList packing = uploadPackingList;
            if(packing == null)
                return;
            
            System.arraycopy(EMPTY_STARTS, 0, pipelineStarts, 0, PipelineManager.MAX_PIPELINES);
            
            final ExpandableByteBuffer buffer = BufferStore.claim();
            buffer.expand(packing.totalBytes());
            intBuffer = buffer.intBuffer();
            intBuffer.position(0);
            
            packing.forEach(this);
            
            buffer.byteBuffer().limit(packing.totalBytes());
            uploadBuffer = buffer;
        }
        
        @Override
        public final void accept(RenderPipeline pipeline, int vertexCount)
        {
            final int pipelineIndex = pipeline.getIndex();
            final int startInt = pipelineStarts[pipelineIndex];
            final int intLength = vertexCount * pipeline.piplineVertexFormat().stride / 4;
            intBuffer.put(pipelineArray[pipelineIndex].rawData(), startInt, intLength);
            pipelineStarts[pipelineIndex] = startInt + intLength;
        }            

    }
    
    private final Consumer packingConsumer = new Consumer();
    
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
      final float relativeX = RenderCube.renderCubeRelative(x);
      final float relativeY = RenderCube.renderCubeRelative(y);
      final float relativeZ = RenderCube.renderCubeRelative(z);
        VertexPackingList packing = new VertexPackingList();
        packing = new VertexPackingList();

        final PriorityQueue<VertexCollector> sorter = sorters.get();
        
        for(VertexCollector collector : pipelineArray)
        {
            if(collector != null)
            {
                collector.sortQuads(relativeX, relativeY, relativeZ);
                sorter.add(collector);
            }
        }
        
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
        
        this.uploadPackingList = packing;
    }
}
