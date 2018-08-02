package grondag.acuity.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelineShaderManager
{
    IPipelineVertexShader getDefaultVertexShader(TextureFormat textureFormat);
    
    IPipelineFragmentShader getDefaultFragmentShader(TextureFormat textureFormat);
    
    IPipelineVertexShader getOrCreateVertexShader(String shaderFileName, TextureFormat textureFormat);
    
    IPipelineFragmentShader getOrCreateFragmentShader(String shaderFileName, TextureFormat textureFormat);
}
