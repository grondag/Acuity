package grondag.render_hooks.api.impl;

import javax.annotation.Nonnull;

import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelineManager implements IPipelineManager
{
    @SuppressWarnings("unchecked")
    private ObjectArrayList<IRenderPipeline>[] pipelines = new ObjectArrayList[BlockRenderLayer.values().length];
    
    public PipelineManager()
    {
        super();
        for(BlockRenderLayer b : BlockRenderLayer.values())
        {
            pipelines[b.ordinal()] = new ObjectArrayList<IRenderPipeline>(MAX_PIPELINES_PER_RENDER_LAYER);
            createPipeline(new VanillaPipeline(b));
        }
    }
    
    @Override
    public synchronized boolean createPipeline(IRenderPipeline pipeline)
    {
        ObjectArrayList<IRenderPipeline> list = pipelines[pipeline.renderLayer().ordinal()];
        final int n = list.size();
        if(n < MAX_PIPELINES_PER_RENDER_LAYER)
        {
            list.add(pipeline);
            pipeline.assignIndex(n);
            return true;
        }
        else
            return false;
    }
    
    public IRenderPipeline getPipeline(@Nonnull BlockRenderLayer layer, int pipelineIndex)
    {
        return pipelines[layer.ordinal()].get(pipelineIndex);
    }

    @Override
    public IRenderPipeline getVanillaPipeline(BlockRenderLayer forLayer)
    {
        return pipelines[forLayer.ordinal()].get(VANILLA_MC_PIPELINE_INDEX);
    }
   
}
