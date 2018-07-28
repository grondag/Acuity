package grondag.acuity.api;

import javax.annotation.Nonnull;

import grondag.acuity.api.IPipelineFragmentShader;
import grondag.acuity.api.IPipelineShaderManager;
import grondag.acuity.api.IPipelineVertexShader;
import grondag.acuity.api.TextureFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class PipelineShaderManager implements IPipelineShaderManager
{
    final static PipelineShaderManager INSTANCE = new PipelineShaderManager();
    private Object2ObjectOpenHashMap<String, PipelineVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<String, PipelineFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

    private final IPipelineVertexShader[] defaultVertex = new IPipelineVertexShader[TextureFormat.values().length];
    private final IPipelineFragmentShader[] defaultFragment = new IPipelineFragmentShader[TextureFormat.values().length];;
    
    String vertexLibrarySource;
    String fragmentLibrarySource;
    
    @SuppressWarnings("null")
    PipelineShaderManager()
    {
        this.defaultVertex[TextureFormat.SINGLE.ordinal()] = this.getOrCreateVertexShader("/assets/acuity/shader/default_single.vert", TextureFormat.SINGLE);
        this.defaultVertex[TextureFormat.DOUBLE.ordinal()] = this.getOrCreateVertexShader("/assets/acuity/shader/default_double.vert", TextureFormat.DOUBLE);
        this.defaultVertex[TextureFormat.TRIPLE.ordinal()] = this.getOrCreateVertexShader("/assets/acuity/shader/default_triple.vert", TextureFormat.TRIPLE);
        this.defaultFragment[TextureFormat.SINGLE.ordinal()] = this.getOrCreateFragmentShader("/assets/acuity/shader/default_single.frag", TextureFormat.SINGLE);
        this.defaultFragment[TextureFormat.DOUBLE.ordinal()] = this.getOrCreateFragmentShader("/assets/acuity/shader/default_double.frag", TextureFormat.DOUBLE);
        this.defaultFragment[TextureFormat.TRIPLE.ordinal()] = this.getOrCreateFragmentShader("/assets/acuity/shader/default_triple.frag", TextureFormat.TRIPLE);
    
        this.loadLibrarySources();
    }
    
    private void loadLibrarySources()
    {
        String commonSource = AbstractPipelineShader.getShaderSource("/assets/acuity/shader/common_lib.glsl");
        this.vertexLibrarySource = AbstractPipelineShader.getShaderSource("/assets/acuity/shader/vertex_lib.glsl") + commonSource;
        this.fragmentLibrarySource = AbstractPipelineShader.getShaderSource("/assets/acuity/shader/fragment_lib.glsl") + commonSource;
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
        this.loadLibrarySources();
        this.fragmentShaders.values().forEach(s -> s.forceReload());
        this.vertexShaders.values().forEach(s -> s.forceReload());
    }
}
