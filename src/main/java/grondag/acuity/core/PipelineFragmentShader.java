package grondag.acuity.core;

import grondag.acuity.api.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.OpenGlHelper;

@Environment(EnvType.CLIENT)
public final class PipelineFragmentShader extends AbstractPipelineShader
{
    PipelineFragmentShader(String fileName, TextureFormat textureFormat, boolean isSolidLayer)
    {
        super(fileName, OpenGlHelper.GL_FRAGMENT_SHADER, textureFormat, isSolidLayer);
    }
    
    @Override
    public String getSource()
    {
        return buildSource(PipelineShaderManager.INSTANCE.fragmentLibrarySource);
    }
}
