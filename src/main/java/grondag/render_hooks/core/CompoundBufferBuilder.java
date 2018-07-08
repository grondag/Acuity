package grondag.render_hooks.core;


import org.lwjgl.opengl.GL11;

import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundBufferBuilder extends BufferBuilder
{
    private static BufferBuilder[] EMPTY_ARRAY = new BufferBuilder[IPipelineManager.MAX_PIPELINES_PER_RENDER_LAYER];
    
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
    
    private BufferBuilder[] pipelineBuffers = new BufferBuilder[IPipelineManager.MAX_PIPELINES_PER_RENDER_LAYER];
    
    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(bufferSizeIn);
        childBuffers.add(this);
    }

    @Override
    public void begin(int glMode, VertexFormat format)
    {
        super.begin(glMode, format);
        System.arraycopy(EMPTY_ARRAY, 0, pipelineBuffers, 0, IPipelineManager.MAX_PIPELINES_PER_RENDER_LAYER);
        pipelineBuffers[IPipelineManager.VANILLA_MC_PIPELINE_INDEX] = this;
        nextAvailableBufferIndex = IPipelineManager.FIRST_CUSTOM_PIPELINE_INDEX;
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
        result.begin(GL11.GL_QUADS, pipeline.vertexFormat());
        result.setTranslation(this.xOffset, this.yOffset, this.zOffset);
        return result;
    }

    @Override
    public void finishDrawing()
    {
        super.finishDrawing();
        if(nextAvailableBufferIndex > IPipelineManager.FIRST_CUSTOM_PIPELINE_INDEX)
        {
            for(int i = IPipelineManager.FIRST_CUSTOM_PIPELINE_INDEX; i < nextAvailableBufferIndex; i++)
            {
                this.childBuffers.get(i).finishDrawing();
            }
        }
    }
}
