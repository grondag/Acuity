package grondag.render_hooks.core;


import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class CompoundBufferBuilder extends BufferBuilder
{
    private static BufferBuilder[] EMPTY_ARRAY = new BufferBuilder[IPipelineManager.MAX_PIPELINE_COUNT];
    
    /**
     * Cache all instantiated buffers for reuse.<p>
     *   
     * Buffers at beginning of list are in use, those at and after {@link #nextAvailableBufferIndex} are available.
     */
    private ObjectArrayList<BufferBuilder> childBuffers = new ObjectArrayList<>();
    
    /**
     * Index of next available buffer in {@link #childBuffers}
     */
    private int nextAvailableBufferIndex;
    
    private BufferBuilder[] pipelineBuffers = new BufferBuilder[IPipelineManager.MAX_PIPELINE_COUNT];
    
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
    
    public BufferBuilder getPipelineBuffer(IRenderPipeline pipeline)
    {
        BufferBuilder result = pipelineBuffers[pipeline.getIndex()];
        if(result == null)
        {
            result = getInitializedBuffer(pipeline);
            pipelineBuffers[pipeline.getIndex()] = result;
        }
        return result;
    }
    
    private BufferBuilder getInitializedBuffer(IRenderPipeline pipeline)
    {
        BufferBuilder result;
        
        if(nextAvailableBufferIndex < childBuffers.size())
        {
            result = childBuffers.get(nextAvailableBufferIndex++);
        }
        else
        {
            result = new BufferBuilder(1024);
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
        for(BufferBuilder b : this.pipelineBuffers)
        {
            if(b != null)
                b.finishDrawing();
        }
    }
}
