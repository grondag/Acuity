package grondag.acuity.core;

import com.mojang.blaze3d.platform.GLX;

import grondag.acuity.api.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PipelineVertexShader  extends AbstractPipelineShader
{
    PipelineVertexShader(String fileName, TextureFormat textureFormat, boolean isSolidLayer)
    {
        super(fileName, GLX.GL_VERTEX_SHADER, textureFormat, isSolidLayer);
    }
    
    @Override
    public String getSource()
    {
        return buildSource(PipelineShaderManager.INSTANCE.vertexLibrarySource);
    }
}
