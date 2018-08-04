package grondag.acuity.core;

import grondag.acuity.api.TextureFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PipelineShaderManager
{
    public final static PipelineShaderManager INSTANCE = new PipelineShaderManager();
    private Object2ObjectOpenHashMap<String, PipelineVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<String, PipelineFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

    private final PipelineVertexShader[] defaultVertex = new PipelineVertexShader[TextureFormat.values().length];
    private final PipelineFragmentShader[] defaultFragment = new PipelineFragmentShader[TextureFormat.values().length];;
    
    String vertexLibrarySource;
    String fragmentLibrarySource;
    
    @SuppressWarnings("null")
    PipelineShaderManager()
    {
        this.defaultVertex[TextureFormat.SINGLE.ordinal()] = this.getOrCreateVertexShader("/assets/acuity/shader/default.vert", TextureFormat.SINGLE);
        this.defaultVertex[TextureFormat.DOUBLE.ordinal()] = this.getOrCreateVertexShader("/assets/acuity/shader/default.vert", TextureFormat.DOUBLE);
        this.defaultVertex[TextureFormat.TRIPLE.ordinal()] = this.getOrCreateVertexShader("/assets/acuity/shader/default.vert", TextureFormat.TRIPLE);
        this.defaultFragment[TextureFormat.SINGLE.ordinal()] = this.getOrCreateFragmentShader("/assets/acuity/shader/default.frag", TextureFormat.SINGLE);
        this.defaultFragment[TextureFormat.DOUBLE.ordinal()] = this.getOrCreateFragmentShader("/assets/acuity/shader/default.frag", TextureFormat.DOUBLE);
        this.defaultFragment[TextureFormat.TRIPLE.ordinal()] = this.getOrCreateFragmentShader("/assets/acuity/shader/default.frag", TextureFormat.TRIPLE);
    
        this.loadLibrarySources();
    }
    
    private void loadLibrarySources()
    {
        String commonSource = AbstractPipelineShader.getShaderSource("/assets/acuity/shader/common_lib.glsl");
        this.vertexLibrarySource = AbstractPipelineShader.getShaderSource("/assets/acuity/shader/vertex_lib.glsl") + commonSource;
        this.fragmentLibrarySource = AbstractPipelineShader.getShaderSource("/assets/acuity/shader/fragment_lib.glsl") + commonSource;
    }
    
    public PipelineVertexShader getOrCreateVertexShader(String shaderFileName, TextureFormat textureFormat)
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

    public PipelineFragmentShader getOrCreateFragmentShader(String shaderFileName, TextureFormat textureFormat)
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

    
    public PipelineVertexShader getDefaultVertexShader(TextureFormat textureFormat)
    {
        return this.defaultVertex[textureFormat.ordinal()];
    }

    
    public PipelineFragmentShader getDefaultFragmentShader(TextureFormat textureFormat)
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
