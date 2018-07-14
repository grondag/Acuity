package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.OpenGlHelper;

public final class PipelineFragmentShaderImpl extends AbstractPipelineShader implements IPipelineFragmentShader
{
    PipelineFragmentShaderImpl(@Nonnull String fileName)
    {
        super(fileName, OpenGlHelper.GL_FRAGMENT_SHADER);
    }
}
