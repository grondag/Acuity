package grondag.render_hooks.core;

import java.util.function.Consumer;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IPipelinedBakedQuad;
import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.model.pipeline.BlockInfo;

public class CompoundVertexLighter implements Consumer<IPipelinedBakedQuad>
{

    protected final BlockInfo blockInfo;
    
    private PipelinedVertexLighter[] lighters = new PipelinedVertexLighter[IPipelineManager.MAX_PIPELINE_COUNT];
    
    private class ChildLighter extends PipelinedVertexLighter
    {
        protected ChildLighter(IRenderPipeline pipeline)
        {
            super(pipeline);
        }

        @Override
        public final BlockInfo getBlockInfo()
        {
            return blockInfo;
        }
    }
    
    @Override
    public void accept(IPipelinedBakedQuad quad)
    {
        getPipelineLighter(quad.getPipelineIndex()).accept(quad);
    }
    
    public CompoundVertexLighter()
    {
        this.blockInfo = new BlockInfo(Minecraft.getMinecraft().getBlockColors());
    }

    public PipelinedVertexLighter getPipelineLighter(IRenderPipeline pipeline)
    {
        PipelinedVertexLighter result = lighters[pipeline.getIndex()];
        if(result == null)
        {
            result = new ChildLighter(pipeline);
            lighters[pipelineIndex] = result;
        }
        return result;
    }
    
  
}
