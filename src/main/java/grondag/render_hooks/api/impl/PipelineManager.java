package grondag.render_hooks.api.impl;

import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelineManager implements IPipelineManager
{
    public static final IRenderPipeline VANILLA_PIPELINE = new VanillaPipeline();
    
    private ObjectArrayList<IRenderPipeline> pipelines = new ObjectArrayList<>(MAX_PIPELINES);
    
    public PipelineManager()
    {
        pipelines.add(VANILLA_PIPELINE);
    }
    
    @Override
    public synchronized boolean createPipeline(IRenderPipeline pipeline)
    {
        final int n = pipelines.size();
        if(n < MAX_PIPELINES)
        {
            pipelines.add(pipeline);
            pipeline.assignIndex(n);
            return true;
        }
        else
            return false;
    }
    
    public IRenderPipeline getPipeline(int pipelineIndex)
    {
        return pipelines.get(pipelineIndex);
    }

    @Override
    public IRenderPipeline getVanillaPipeline()
    {
        return VANILLA_PIPELINE;
    }
   
}
