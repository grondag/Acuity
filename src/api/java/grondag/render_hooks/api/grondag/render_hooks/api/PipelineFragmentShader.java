package grondag.render_hooks.api;

import javax.annotation.Nonnull;

public abstract class PipelineFragmentShader extends AbstractPipelineShader
{
    PipelineFragmentShader(@Nonnull String fileName)
    {
        super(fileName);
    }
}
