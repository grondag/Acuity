package grondag.render_hooks.core;

import grondag.render_hooks.api.IRenderPipeline;

public class EnhancedVertexLighter extends CompoundVertexLighter
{

    @SuppressWarnings("null")
    @Override
    protected PipelinedVertexLighter createChildLighter(IRenderPipeline pipeline)
    {
        // TODO implement enhanced lighting
        return null;
    }

}
