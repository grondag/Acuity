package grondag.render_hooks.api.impl;

import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelineManager implements IPipelineManager
{
    private ObjectArrayList<IRenderPipeline> list = new ObjectArrayList<>();
    
    @Override
    public synchronized boolean createPipeline(IRenderPipeline pipeline)
    {
        final int n = list.size();
        if(n < MAX_PIPELINE_COUNT)
        {
            list.add(pipeline);
            pipeline.assignIndex(n);
            return true;
        }
        else
            return false;
    }
    
    public IRenderPipeline getPipeline(int pipelineIndex)
    {
        return list.get(pipelineIndex);
    }
    
    public int materialCount()
    {
        return list.size();
    }
}
