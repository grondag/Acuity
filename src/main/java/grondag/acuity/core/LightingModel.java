package grondag.acuity.core;

import static grondag.acuity.pipeline.PipelineVertexFormat.*;

import grondag.acuity.api.TextureDepth;
import grondag.acuity.pipeline.PipelineVertexFormat;

public enum LightingModel
{
    CLASSIC(VANILLA_SINGLE, VANILLA_DOUBLE, VANILLA_TRIPLE);
    
//    EHNANCED(ENHANCED_SINGLE, ENHANCED_DOUBLE, ENHANCED_TRIPLE)
//    {
//        @Override
//        public CompoundVertexLighter createLighter()
//        {
//            return new EnhancedVertexLighter();
//        }
//    };
    
    private LightingModel(PipelineVertexFormat... formatMap)
    {
        this.formatMap = formatMap;
    }
    
    private final PipelineVertexFormat[] formatMap;
    
    public PipelineVertexFormat vertexFormat(TextureDepth textureFormat)
    {
        return formatMap[textureFormat.ordinal()];
    }

    public CompoundVertexLighter createLighter()
    {
        return new VanillaVertexLighter();
    }
}
