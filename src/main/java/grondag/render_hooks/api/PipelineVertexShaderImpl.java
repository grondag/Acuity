package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.OpenGlHelper;

public final class PipelineVertexShaderImpl  extends AbstractPipelineShader implements IPipelineVertexShader
{
    PipelineVertexShaderImpl(@Nonnull String fileName)
    {
        super(fileName, OpenGlHelper.GL_VERTEX_SHADER);
    }
}
