package grondag.render_hooks.api;

import javax.annotation.Nonnull;

abstract class AbstractPipelineShader
{
    final String fileName;
    
    AbstractPipelineShader(@Nonnull String fileName)
    {
        this.fileName = fileName;
    }
}
