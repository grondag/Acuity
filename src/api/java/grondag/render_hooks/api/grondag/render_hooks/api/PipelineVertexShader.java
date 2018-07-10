package grondag.render_hooks.api;

import javax.annotation.Nonnull;

public abstract class PipelineVertexShader extends AbstractPipelineShader
{
    PipelineVertexShader(@Nonnull String fileName)
    {
        super(fileName);
    }
}
