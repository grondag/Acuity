package grondag.acuity.core;

import com.mojang.blaze3d.platform.GLX;

import grondag.acuity.api.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PipelineFragmentShader extends AbstractPipelineShader
{
    PipelineFragmentShader(String fileName, TextureFormat textureFormat, boolean isSolidLayer)
    {
        super(fileName, GLX.GL_FRAGMENT_SHADER, textureFormat, isSolidLayer);
    }
    
    @Override
    public String getSource()
    {
        return buildSource(PipelineShaderManager.INSTANCE.fragmentLibrarySource);
    }
}
