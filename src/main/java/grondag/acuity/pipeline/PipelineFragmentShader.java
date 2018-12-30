package grondag.acuity.pipeline;

import com.mojang.blaze3d.platform.GLX;

import grondag.acuity.api.TextureDepth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PipelineFragmentShader extends AbstractPipelineShader
{
    PipelineFragmentShader(String fileName, TextureDepth textureFormat, boolean isSolidLayer)
    {
        super(fileName, GLX.GL_FRAGMENT_SHADER, textureFormat, isSolidLayer);
    }
    
    @Override
    public String getSource()
    {
        return buildSource(PipelineShaderManager.INSTANCE.fragmentLibrarySource);
    }
}
