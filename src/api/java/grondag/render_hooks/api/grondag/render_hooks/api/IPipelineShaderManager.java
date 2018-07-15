package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IPipelineShaderManager
{
    
    IPipelineVertexShader getDefaultVertexShader(PipelineVertexFormat format);
    
    IPipelineFragmentShader getDefaultFragmentShader(PipelineVertexFormat format);
    
    @Nullable
    IPipelineVertexShader getOrCreateVertexShader(@Nonnull String shaderFileName);
    
    @Nullable
    IPipelineFragmentShader getOrCreateFragmentShader(@Nonnull String shaderFileName);
}
