package grondag.acuity.api;

import javax.annotation.Nonnull;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelineShaderManager
{
    IPipelineVertexShader getDefaultVertexShader(TextureFormat textureFormat);
    
    IPipelineFragmentShader getDefaultFragmentShader(TextureFormat textureFormat);
    
    IPipelineVertexShader getOrCreateVertexShader(@Nonnull String shaderFileName, TextureFormat textureFormat);
    
    IPipelineFragmentShader getOrCreateFragmentShader(@Nonnull String shaderFileName, TextureFormat textureFormat);
}
