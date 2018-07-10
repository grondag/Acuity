package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

final class PipelineShaderManagerImpl extends PipelineShaderManager
{
    PipelineShaderManagerImpl() {}

    private Object2ObjectOpenHashMap<String, PipelineVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    
    private Object2ObjectOpenHashMap<String, PipelineFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();
    
    @Override
    public @Nullable PipelineVertexShader getOrCreateVertexShader(@Nonnull String shaderFileName)
    {
        if(shaderFileName == null || shaderFileName.isEmpty()) 
            return null;
        
        synchronized(vertexShaders)
        {
            PipelineVertexShader result = vertexShaders.get(shaderFileName);
            if(result == null)
            {
                result = new PipelineVertexShaderImpl(shaderFileName);
                vertexShaders.put(shaderFileName, result);
            }
            return result;
        }
    }

    @Override
    public @Nullable PipelineFragmentShader getOrCreateFragmentShader(@Nonnull String shaderFileName)
    {
        if(shaderFileName == null || shaderFileName.isEmpty()) 
            return null;
        
        synchronized(fragmentShaders)
        {
            PipelineFragmentShader result = fragmentShaders.get(shaderFileName);
            if(result == null)
            {
                result = new PipelineFragmentShaderImpl(shaderFileName);
                fragmentShaders.put(shaderFileName, result);
            }
            return result;
        }
    }
}
