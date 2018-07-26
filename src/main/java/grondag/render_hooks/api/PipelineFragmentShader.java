package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.OpenGlHelper;

public final class PipelineFragmentShader extends AbstractPipelineShader implements IPipelineFragmentShader
{
    PipelineFragmentShader(@Nonnull String fileName, @Nonnull TextureFormat textureFormat)
    {
        super(fileName, OpenGlHelper.GL_FRAGMENT_SHADER, textureFormat);
    }
    
    @Override
    public String getSource()
    {
        String result = super.getSource();
        result = result.replaceAll("#version\\s+120", "");
        return PipelineShaderManager.INSTANCE.fragmentLibrarySource + result;
    }
}
