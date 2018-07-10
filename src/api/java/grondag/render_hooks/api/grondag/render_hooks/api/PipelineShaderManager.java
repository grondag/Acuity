package grondag.render_hooks.api;

import javax.annotation.Nonnull;

public abstract class PipelineShaderManager
{
    PipelineShaderManager() {}
    
    public abstract PipelineVertexShader getOrCreateVertexShader(@Nonnull String shaderFileName);
    
    public abstract PipelineFragmentShader getOrCreateFragmentShader(@Nonnull String shaderFileName);
}
