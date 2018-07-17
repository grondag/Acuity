package grondag.render_hooks.api;

import javax.annotation.Nonnull;

public interface IPipelineShaderManager
{
    
    IPipelineVertexShader getDefaultVertexShader(PipelineVertexFormat format);
    
    IPipelineFragmentShader getDefaultFragmentShader(PipelineVertexFormat format);
    
    IPipelineVertexShader getOrCreateVertexShader(@Nonnull String shaderFileName);
    
    IPipelineFragmentShader getOrCreateFragmentShader(@Nonnull String shaderFileName);
}
