package grondag.render_hooks.core;

import org.lwjgl.opengl.GL11;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.PipelineVertexFormat;
import grondag.render_hooks.api.RenderHookRuntime;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundBufferBuilder extends BufferBuilder
{
    private static final BufferBuilder[] EMPTY_ARRAY = new BufferBuilder[IPipelineManager.MAX_PIPELINES];
    
    private final IRenderPipeline VANILLA_PIPELINE = RenderHookRuntime.INSTANCE.getPipelineManager().getDefaultPipeline(PipelineVertexFormat.SINGLE);
    
    /**
     * Cache all instantiated buffers for reuse. Does not include this instance<p>
     */
    private ObjectArrayList<BufferBuilder> childBuffers = new ObjectArrayList<>();
    
    /**
     * Track pipelines in use as list for fast upload 
     * and to know if we ned to allocate more.  Never includes the vanilla pipeline.
     */
    private ObjectArrayList<IRenderPipeline> pipelineList = new ObjectArrayList<>();
    
    /**
     * Fast lookup of buffers by pipeline index.  Element 0 will always be this.
     */
    BufferBuilder[] pipelineArray = new BufferBuilder[IPipelineManager.MAX_PIPELINES];
    
    private int totalBytes = 0;
    
    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(bufferSizeIn);
    }
    
    @Override
    public void begin(int glMode, VertexFormat format)
    {
        // UGLY:  means this class can only be used for chunk rebuilds
        // one alternative would be to honor input format but then create separate buffers - wasteful
     
        super.begin(glMode, RenderHooks.isModEnabled() ? PipelineVertexFormat.SINGLE.vertexFormat : format);
        pipelineList.clear();
        this.totalBytes = 0;
        System.arraycopy(EMPTY_ARRAY, 0, pipelineArray, 0, IPipelineManager.MAX_PIPELINES);
        pipelineArray[IPipelineManager.VANILLA_MC_PIPELINE_INDEX] = this;
    }
    
    public BufferBuilder getPipelineBuffer(IRenderPipeline pipeline)
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
    
    private BufferBuilder getInitializedBuffer(IRenderPipeline pipeline)
    {
        BufferBuilder result;
        
        final int count = pipelineList.size();
        if(count < childBuffers.size())
        {
            result = childBuffers.get(count);
        }
        else
        {
            result = new BufferBuilder(1024);
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
        this.totalBytes = this.byteBuffer.limit();
        
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
        if(this.vertexCount > 0)
        {
            target.uploadBuffer(VANILLA_PIPELINE, this.getByteBuffer());
            super.reset();
        }
        if(!pipelineList.isEmpty())
            pipelineList.forEach(p -> target.uploadBuffer(p, pipelineArray[p.getIndex()].getByteBuffer()));
        
        target.completeUpload();
    }

    public void uploadTo(CompoundListedRenderChunk target, int vanillaList)
    {
        if(this.vertexCount == 0 && pipelineList.isEmpty())
            return;
        
        target.prepareForUpload(vanillaList);
        if(this.vertexCount > 0)
        {
            target.uploadBuffer(VANILLA_PIPELINE, this);
            super.reset();
        }
        if(!pipelineList.isEmpty())
            pipelineList.forEach(p -> target.uploadBuffer(p, pipelineArray[p.getIndex()]));
        
        target.completeUpload();
    }
}
