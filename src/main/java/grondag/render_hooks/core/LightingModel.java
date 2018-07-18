package grondag.render_hooks.core;

import grondag.render_hooks.api.TextureFormat;
import static grondag.render_hooks.core.PipelineVertexFormat.*;

public enum LightingModel
{
    CLASSIC(VANILLA_SINGLE, VANILLA_DOUBLE, VANILLA_TRIPLE),
    EHNANCED(ENHANCED_SINGLE, ENHANCED_DOUBLE, ENHANCED_TRIPLE)
    {
        @Override
        public CompoundVertexLighter createLighter()
        {
            return new EnhancedVertexLighter();
        }
    };
    
    private LightingModel(PipelineVertexFormat... formatMap)
    {
        this.formatMap = formatMap;
    }
    
    private final PipelineVertexFormat[] formatMap;
    
    public PipelineVertexFormat vertexFormat(TextureFormat textureFormat)
    {
        return formatMap[textureFormat.ordinal()];
    }

    public CompoundVertexLighter createLighter()
    {
        return new CompoundVertexLighter();
    }
}
