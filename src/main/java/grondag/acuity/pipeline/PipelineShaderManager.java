package grondag.acuity.pipeline;

import grondag.acuity.api.TextureDepth;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PipelineShaderManager
{
    public final static PipelineShaderManager INSTANCE = new PipelineShaderManager();
    private Object2ObjectOpenHashMap<String, PipelineVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<String, PipelineFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

    String vertexLibrarySource;
    String fragmentLibrarySource;
    
    public final String DEFAULT_VERTEX_SOURCE = "/assets/acuity/shader/default.vert";
    public final String DEFAULT_FRAGMENT_SOURCE = "/assets/acuity/shader/default.frag";
    
    PipelineShaderManager()
    {
        this.loadLibrarySources();
    }
    
    private void loadLibrarySources()
    {
        String commonSource = AbstractPipelineShader.getShaderSource("/assets/acuity/shader/common_lib.glsl");
        this.vertexLibrarySource = commonSource + AbstractPipelineShader.getShaderSource("/assets/acuity/shader/vertex_lib.glsl");
        this.fragmentLibrarySource = commonSource + AbstractPipelineShader.getShaderSource("/assets/acuity/shader/fragment_lib.glsl");
    }
    
    private String shaderKey(String shaderFileName, TextureDepth textureFormat, boolean isSolidLayer)
    {
        return String.format("%s.%s.%s", shaderFileName, textureFormat, isSolidLayer);
    }
    
    public PipelineVertexShader getOrCreateVertexShader(String shaderFileName, TextureDepth textureFormat, boolean isSolidLayer)
    {
        final String shaderKey = shaderKey(shaderFileName, textureFormat, isSolidLayer);
        
        synchronized(vertexShaders)
        {
            PipelineVertexShader result = vertexShaders.get(shaderKey);
            if(result == null)
            {
                result = new PipelineVertexShader(shaderFileName, textureFormat, isSolidLayer);
                vertexShaders.put(shaderKey, result);
            }
            return result;
        }
    }

    public PipelineFragmentShader getOrCreateFragmentShader(String shaderFileName, TextureDepth textureFormat, boolean isSolidLayer)
    {
        final String shaderKey = shaderKey(shaderFileName, textureFormat, isSolidLayer);
        
        synchronized(fragmentShaders)
        {
            PipelineFragmentShader result = fragmentShaders.get(shaderKey);
            if(result == null)
            {
                result = new PipelineFragmentShader(shaderFileName, textureFormat, isSolidLayer);
                fragmentShaders.put(shaderKey, result);
            }
            return result;
        }
    }

    public void forceReload()
    {
        this.loadLibrarySources();
        this.fragmentShaders.values().forEach(s -> s.forceReload());
        this.vertexShaders.values().forEach(s -> s.forceReload());
    }
}
