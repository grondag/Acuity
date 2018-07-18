package grondag.render_hooks.api;

import javax.annotation.Nonnull;

public interface IPipelineShaderManager
{
    
    IPipelineVertexShader getDefaultVertexShader(TextureFormat textureFormat);
    
    IPipelineFragmentShader getDefaultFragmentShader(TextureFormat textureFormat);
    
    IPipelineVertexShader getOrCreateVertexShader(@Nonnull String shaderFileName, TextureFormat textureFormat);
    
    IPipelineFragmentShader getOrCreateFragmentShader(@Nonnull String shaderFileName, TextureFormat textureFormat);
}
