package grondag.render_hooks.api;

import javax.annotation.Nullable;

import grondag.render_hooks.api.PipelineManager;
import grondag.render_hooks.api.RenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PipelineManagerImpl extends PipelineManager
{
    private ObjectArrayList<RenderPipeline> pipelines = new ObjectArrayList<>(MAX_PIPELINES);
    
    PipelineManagerImpl()
    {
        super();
        pipelines.add(RenderPipelineImpl.VANILLA_PIPELINE);
    }
    
    @Nullable
    @Override
    public final synchronized RenderPipeline createPipeline(PipelineVertexFormat format)
    {
        final int n = pipelines.size();
        if(n < MAX_PIPELINES)
        {
            RenderPipelineImpl result = new RenderPipelineImpl(format);
            pipelines.add(result);
            return result;
        }
        else
            return null;
    }
    
    public final RenderPipeline getPipeline(int pipelineIndex)
    {
        return pipelines.get(pipelineIndex);
    }

    @Override
    public final RenderPipeline getVanillaPipeline()
    {
        return RenderPipelineImpl.VANILLA_PIPELINE;
    }

    @Override
    public VertexFormat getVertexFormat(RenderPipeline pipeline)
    {
        // TODO Auto-generated method stub
        return null;
    }
   
}
