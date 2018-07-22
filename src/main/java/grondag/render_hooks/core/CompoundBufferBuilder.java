package grondag.render_hooks.core;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

import org.lwjgl.opengl.GL11;

import com.google.common.primitives.Floats;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.RenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundBufferBuilder extends BufferBuilder
{
    private static final BufferBuilder[] EMPTY_ARRAY = new BufferBuilder[IPipelineManager.MAX_PIPELINES];
    
    /**
     * Cache all instantiated buffers for reuse. Does not include this instance<p>
     */
    private ObjectArrayList<BufferBuilder> childBuffers = new ObjectArrayList<>();
    
    /**
     * Track pipelines in use as list for fast upload 
     * and to know if we ned to allocate more.  Never includes the vanilla pipeline.
     */
    private ObjectArrayList<RenderPipeline> pipelineList = new ObjectArrayList<>();
    
    /**
     * Fast lookup of buffers by pipeline index.  Element 0 will always be this.
     */
    BufferBuilder[] pipelineArray = new BufferBuilder[IPipelineManager.MAX_PIPELINES];
    
    private int totalBytes = 0;
    
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
        pipelineList.clear();
        this.totalBytes = 0;
        System.arraycopy(EMPTY_ARRAY, 0, pipelineArray, 0, IPipelineManager.MAX_PIPELINES);
    }
    
    public BufferBuilder getPipelineBuffer(RenderPipeline pipeline)
    {
        final int i = pipeline.getIndex();
        BufferBuilder result = pipelineArray[i];
        if(result == null)
        {
            result = getInitializedBuffer(pipeline);
            pipelineArray[i] = result;
            pipelineList.add(pipeline);
        }
        return result;
    }
    
    private BufferBuilder getInitializedBuffer(RenderPipeline pipeline)
    {
        BufferBuilder result;
        
        final int count = pipelineList.size();
        if(count < childBuffers.size())
        {
            result = childBuffers.get(count);
        }
        else
        {
            // UGLY: this size may be too small and buffer growth logic 
            // grows only in multiples of 2MB - needs profiling
            result = new BufferBuilder(512000);
            childBuffers.add(result);
        }
        result.begin(GL11.GL_QUADS, pipeline.vertexFormat());
        result.setTranslation(this.xOffset, this.yOffset, this.zOffset);
        return result;
    }

    @Override
    public void finishDrawing()
    {
        super.finishDrawing();
        
        if(!pipelineList.isEmpty())
            pipelineList.forEach(p -> 
            {
                final BufferBuilder b = pipelineArray[p.getIndex()];
                b.finishDrawing();
                this.totalBytes += b.getByteBuffer().limit();
            });
    }

    public void uploadTo(CompoundVertexBuffer target)
    {
        target.prepareForUpload(this.totalBytes);
        if(!pipelineList.isEmpty())
            pipelineList.forEach(p -> target.uploadBuffer(p, pipelineArray[p.getIndex()].getByteBuffer()));
        
        target.completeUpload();
    }

    
    //TODO: for display lists need to refactor for new design where the super instance isn't used
    public void uploadTo(CompoundListedRenderChunk target, int vanillaList)
    {
        if(this.vertexCount == 0 && pipelineList.isEmpty())
            return;
        
        target.prepareForUpload(vanillaList);
//        if(this.vertexCount > 0)
//        {
//            target.uploadBuffer(VANILLA_PIPELINE, this);
//            super.reset();
//        }
        if(!pipelineList.isEmpty())
            pipelineList.forEach(p -> target.uploadBuffer(p, pipelineArray[p.getIndex()]));
        
        target.completeUpload();
    }
    
    @Override
    public void sortVertexData(float p_181674_1_, float p_181674_2_, float p_181674_3_)
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
