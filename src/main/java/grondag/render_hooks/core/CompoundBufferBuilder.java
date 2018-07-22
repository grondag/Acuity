package grondag.render_hooks.core;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

import javax.annotation.Nullable;

import com.google.common.primitives.Floats;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.RenderPipeline;
import grondag.render_hooks.core.BufferStore.ExpandableByteBuffer;
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
     * Cache all instantiated buffers for reuse. Does not include this instance<p>
     */
    private ObjectArrayList<VertexCollector> collectors = new ObjectArrayList<>();
    
    /**
     * Track pipelines in use as list for fast upload 
     * and to know if we ned to allocate more.  Never includes the vanilla pipeline.
     */
    private ObjectArrayList<RenderPipeline> pipelineList = new ObjectArrayList<>();
    
    /**
     * Fast lookup of buffers by pipeline index.  Element 0 will always be this.
     */
    VertexCollector[] pipelineArray = new VertexCollector[IPipelineManager.MAX_PIPELINES];
    
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
    
    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(limitBufferSize(bufferSizeIn));
    }
    
    // the RegionRenderCacheBuilder instantiates this with pretty large sizes
    // but in most cases the super instance won't be used when the mod is enabled
    // so don't honor these when mod is enabled to reduce memory footprint
    static final int limitBufferSize(int bufferSizeIn)
    {
        if(RenderHooks.isModEnabled())
        {
            if(bufferSizeIn == 2097152 || bufferSizeIn == 131072 || bufferSizeIn == 262144)
            {
                return 2800;
            }
        }
        return bufferSizeIn;
    }
    
    @Override
    public void begin(int glMode, VertexFormat format)
    {
        super.begin(glMode, format);
        if(RenderHooks.isModEnabled())
        {
            pipelineList.clear();
            System.arraycopy(EMPTY_ARRAY, 0, pipelineArray, 0, IPipelineManager.MAX_PIPELINES);
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
        VertexCollector result;
        
        final int count = pipelineList.size();
        if(count < collectors.size())
        {
            result = collectors.get(count);
        }
        else
        {
            result = new VertexCollector(1024);
            collectors.add(result);
        }
        result.clear();
        return result;
    }

    @Override
    public void finishDrawing()
    {
        super.finishDrawing();
        
        if(RenderHooks.isModEnabled())
        {
            
            final VertexPackingList packing = new VertexPackingList();
            int byteCount = 0;
            if(!pipelineList.isEmpty())
            {
                for(RenderPipeline p : pipelineList)
                {
                    final VertexCollector b = pipelineArray[p.getIndex()];
                    final int byteSize = b.size() * 4;
                    final int vertexCount = byteSize / p.piplineVertexFormat().stride;
                    if(byteSize != 0)
                    {
                        packing.addPacking(p, byteCount, vertexCount);
                        byteCount += byteSize;
                    }
                }
            }
            
            final ExpandableByteBuffer buffer = BufferStore.claim();
            buffer.expand(byteCount);
            final IntBuffer intBuffer = buffer.intBuffer();
            packing.forEach((RenderPipeline p, int byteOffset, int vertexCount) ->
            {
                VertexCollector data = pipelineArray[p.getIndex()];
                intBuffer.position(byteOffset / 4);
                intBuffer.put(data.rawData(), 0, data.size());
            });
            
            this.uploadPackingList = packing;
            this.uploadBuffer = buffer;
            this.uploadBuffer.byteBuffer().limit(byteCount);
        }
    }

    public void uploadTo(CompoundVertexBuffer target)
    {   
        final ExpandableByteBuffer uploadBuffer = this.uploadBuffer;
        final VertexPackingList packingList = this.uploadPackingList;
        if(uploadBuffer != null)
        {
            if(packingList != null)
                target.upload(this.uploadBuffer.byteBuffer(), this.uploadPackingList);
            
            BufferStore.release(this.uploadBuffer);
        }
        this.uploadBuffer = null;
        this.uploadPackingList = null;
    }
    
    //TODO: for display lists need to refactor for new design where the super instance isn't used
    @Deprecated
    public void uploadTo(CompoundListedRenderChunk target, int vanillaList)
    {
//        if(this.vertexCount == 0 && pipelineList.isEmpty())
//            return;
//        
//        target.prepareForUpload(vanillaList);
////        if(this.vertexCount > 0)
////        {
////            target.uploadBuffer(VANILLA_PIPELINE, this);
////            super.reset();
////        }
//        if(!pipelineList.isEmpty())
//            pipelineList.forEach(p -> target.uploadBuffer(p, populateUploadBuffer(pipelineArray[p.getIndex()], p)));
//        
//        target.completeUpload();
    }
    
    @Override
    public void sortVertexData(float x, float y, float z)
    {
        if(RenderHooks.isModEnabled())
            sortCompondVertexData(x, y, z);
        else
            super.sortVertexData(x, y, z);
    }
    
    private void sortCompondVertexData(float p_181674_1_, float p_181674_2_, float p_181674_3_)
    {
        int quadCount = this.vertexCount / 4;
        
        final float[] perQuadDistance = new float[quadCount];

        for (int j = 0; j < quadCount; ++j)
        {
            perQuadDistance[j] = getDistanceSq(this.rawFloatBuffer, (float)((double)p_181674_1_ + this.xOffset), (float)((double)p_181674_2_ + this.yOffset), (float)((double)p_181674_3_ + this.zOffset), this.vertexFormat.getIntegerSize(), j * this.vertexFormat.getNextOffset());
        }

        // assign an index to each quad
        // is a boxed type to support comparator?
        Integer[] quadIndexes = new Integer[quadCount];

        for (int k = 0; k < quadIndexes.length; ++k)
        {
            quadIndexes[k] = k;
        }

        // sort the indexes by distance
        Arrays.sort(quadIndexes, new Comparator<Integer>()
        {
            public int compare(Integer p_compare_1_, Integer p_compare_2_)
            {
                return Floats.compare(perQuadDistance[p_compare_2_.intValue()], perQuadDistance[p_compare_1_.intValue()]);
            }
        });
        
        BitSet bitset = new BitSet();
        int perVertexStride = this.vertexFormat.getNextOffset();
        
        // because stride is bytes and this is an int, holds all data for one quad
        int[] quadDataToMove = new int[perVertexStride];

        for (int targetIndex = bitset.nextClearBit(0); targetIndex < quadIndexes.length; targetIndex = bitset.nextClearBit(targetIndex + 1))
        {
            int sourceIndex = quadIndexes[targetIndex].intValue();

            if (sourceIndex != targetIndex)
            {
                this.rawIntBuffer.limit(sourceIndex * perVertexStride + perVertexStride);
                this.rawIntBuffer.position(sourceIndex * perVertexStride);
                this.rawIntBuffer.get(quadDataToMove);
                int k1 = sourceIndex;

                for (int l1 = quadIndexes[sourceIndex].intValue(); k1 != targetIndex; l1 = quadIndexes[l1].intValue())
                {
                    this.rawIntBuffer.limit(l1 * perVertexStride + perVertexStride);
                    this.rawIntBuffer.position(l1 * perVertexStride);
                    IntBuffer intbuffer = this.rawIntBuffer.slice();
                    this.rawIntBuffer.limit(k1 * perVertexStride + perVertexStride);
                    this.rawIntBuffer.position(k1 * perVertexStride);
                    this.rawIntBuffer.put(intbuffer);
                    bitset.set(k1);
                    k1 = l1;
                }

                this.rawIntBuffer.limit(targetIndex * perVertexStride + perVertexStride);
                this.rawIntBuffer.position(targetIndex * perVertexStride);
                this.rawIntBuffer.put(quadDataToMove);
            }

            bitset.set(targetIndex);
        }
        this.rawIntBuffer.limit(this.rawIntBuffer.capacity());
        this.rawIntBuffer.position(this.getBufferSize());
    }
}
