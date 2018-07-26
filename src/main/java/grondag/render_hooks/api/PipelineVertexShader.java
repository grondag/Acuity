package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.OpenGlHelper;

public final class PipelineVertexShader  extends AbstractPipelineShader implements IPipelineVertexShader
{
    PipelineVertexShader(@Nonnull String fileName, @Nonnull TextureFormat textureFormat)
    {
        super(fileName, OpenGlHelper.GL_VERTEX_SHADER, textureFormat);
    }
    
    @Override
    public String getSource()
    {
        String result = super.getSource();
        result = result.replaceAll("#version\\s+120", "");
        return PipelineShaderManager.INSTANCE.vertexLibrarySource + result;
    }
}
