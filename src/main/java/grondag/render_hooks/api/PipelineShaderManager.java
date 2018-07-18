package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

final class PipelineShaderManager implements IPipelineShaderManager
{
    final static PipelineShaderManager INSTANCE = new PipelineShaderManager();
    private Object2ObjectOpenHashMap<String, PipelineVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<String, PipelineFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

    private final IPipelineVertexShader[] defaultVertex = new IPipelineVertexShader[TextureFormat.values().length];
    private final IPipelineFragmentShader[] defaultFragment = new IPipelineFragmentShader[TextureFormat.values().length];;
    
    public final PipelineVertexShader vertexLibrary;
    
    PipelineShaderManager()
    {
        //FIXME: put in real names for double & triple
        this.defaultVertex[TextureFormat.SINGLE.ordinal()] = this.getOrCreateVertexShader("/assets/render_hooks/shader/default_single.vert", TextureFormat.SINGLE);
        this.defaultVertex[TextureFormat.DOUBLE.ordinal()] = this.getOrCreateVertexShader("/assets/render_hooks/shader/default_single.vert", TextureFormat.DOUBLE);
        this.defaultVertex[TextureFormat.TRIPLE.ordinal()] = this.getOrCreateVertexShader("/assets/render_hooks/shader/default_single.vert", TextureFormat.TRIPLE);
        this.defaultFragment[TextureFormat.SINGLE.ordinal()] = this.getOrCreateFragmentShader("/assets/render_hooks/shader/default_single.frag", TextureFormat.SINGLE);
        this.defaultFragment[TextureFormat.DOUBLE.ordinal()] = this.getOrCreateFragmentShader("/assets/render_hooks/shader/default_single.frag", TextureFormat.DOUBLE);
        this.defaultFragment[TextureFormat.TRIPLE.ordinal()] = this.getOrCreateFragmentShader("/assets/render_hooks/shader/default_single.frag", TextureFormat.TRIPLE);
        
        this.vertexLibrary = (PipelineVertexShader)this.getOrCreateVertexShader("/assets/render_hooks/shader/library.glsl", TextureFormat.SINGLE);
        
    }
    
    @Override
    public IPipelineVertexShader getOrCreateVertexShader(@Nonnull String shaderFileName, @Nonnull TextureFormat textureFormat)
    {
        synchronized(vertexShaders)
        {
            PipelineVertexShader result = vertexShaders.get(shaderFileName);
            if(result == null)
            {
                result = new PipelineVertexShader(shaderFileName, textureFormat);
                vertexShaders.put(shaderFileName, result);
            }
            return result;
        }
    }

    @Override
    public IPipelineFragmentShader getOrCreateFragmentShader(@Nonnull String shaderFileName, @Nonnull TextureFormat textureFormat)
    {
        synchronized(fragmentShaders)
        {
            PipelineFragmentShader result = fragmentShaders.get(shaderFileName);
            if(result == null)
            {
                result = new PipelineFragmentShader(shaderFileName, textureFormat);
                fragmentShaders.put(shaderFileName, result);
            }
            return result;
        }
    }

    @Override
    public IPipelineVertexShader getDefaultVertexShader(TextureFormat textureFormat)
    {
        return this.defaultVertex[textureFormat.ordinal()];
    }

    @Override
    public IPipelineFragmentShader getDefaultFragmentShader(TextureFormat textureFormat)
    {
        return this.defaultFragment[textureFormat.ordinal()];
    }
    
    public void forceReload()
    {
        this.fragmentShaders.values().forEach(s -> s.forceReload());
        this.vertexShaders.values().forEach(s -> s.forceReload());
    }
}