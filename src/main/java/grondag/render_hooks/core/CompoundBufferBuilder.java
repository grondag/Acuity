package grondag.render_hooks.core;


import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.BlockPos;

public class CompoundBufferBuilder extends VertexConsumerBufferBuilder
{
    private static VertexConsumerBufferBuilder[] EMPTY_ARRAY = new VertexConsumerBufferBuilder[IPipelineManager.MAX_PIPELINE_COUNT];
    
    /**
     * Cache all instantiated buffers for reuse.<p>
     *   
     * Buffers at beginning of list are in use, those at and after {@link #nextAvailableBufferIndex} are available.
     */
    private ObjectArrayList<VertexConsumerBufferBuilder> childBuffers = new ObjectArrayList<>();
    
    /**
     * Index of next available buffer in {@link #childBuffers}
     */
    private int nextAvailableBufferIndex;
    
    private VertexConsumerBufferBuilder[] pipelineBuffers = new VertexConsumerBufferBuilder[IPipelineManager.MAX_PIPELINE_COUNT];
    
    private BlockPos offset = BlockPos.ORIGIN;
    
    private class ChildBuffer extends VertexConsumerBufferBuilder
    {
        public ChildBuffer(int bufferSizeIn)
        {
            super(bufferSizeIn);
        }

        @Override
        public BlockPos getOffset()
        {
            return offset;
        }
    }

    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(bufferSizeIn);
    }

    @Override
    public void begin(int glMode, VertexFormat format)
    {
        super.begin(glMode, format);
        System.arraycopy(EMPTY_ARRAY, 0, pipelineBuffers, 0, IPipelineManager.MAX_PIPELINE_COUNT);
        nextAvailableBufferIndex = 0;
    }
    
    public void setOffset(BlockPos offset)
    {
        this.offset = offset.toImmutable();
    }
    
    public VertexConsumerBufferBuilder getMaterialBuffer(IRenderPipeline pipeline)
    {
        VertexConsumerBufferBuilder result = pipelineBuffers[pipeline.getIndex()];
        if(result == null)
        {
            result = getInitializedBuffer(pipeline);
            pipelineBuffers[pipeline.getIndex()] = result;
        }
        return result;
    }
    
    private VertexConsumerBufferBuilder getInitializedBuffer(IRenderPipeline pipeline)
    {
        VertexConsumerBufferBuilder result;
        
        if(nextAvailableBufferIndex < childBuffers.size())
        {
            result = childBuffers.get(nextAvailableBufferIndex++);
        }
        else
        {
            result = new ChildBuffer(1024);
            childBuffers.add(result);
            nextAvailableBufferIndex = childBuffers.size();
        }
        result.begin(pipeline.glMode(), pipeline.vertexFormat());
        result.setTranslation(this.xOffset, this.yOffset, this.zOffset);
        return result;
    }

    @Override
    public void finishDrawing()
    {
        super.finishDrawing();
        for(VertexConsumerBufferBuilder b : this.pipelineBuffers)
        {
            if(b != null)
                b.finishDrawing();
        }
    }

    @Override
    public BlockPos getOffset()
    {
        return this.offset;
    }
    
    
}
