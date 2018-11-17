package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.acuity.hooks.PipelineHooks;
import grondag.acuity.opengl.GLBufferStore;
import net.minecraft.client.renderer.OpenGlHelper;

@Mixin(OpenGlHelper.class)
public abstract class MixinOpenGlHelper
{
    /**
     * @reason Will always use vbo if Acuity is enabled.
     * @author grondag
     */
    @Overwrite
    public static boolean useVbo()
    {
        return PipelineHooks.useVbo();
    }

    /**
     * @reason Reuse GL buffers and claim in batches - on some systems new buffers are expensive.
     * @author grondag
     */
    @Overwrite
    public static int glGenBuffers()
    {
        return GLBufferStore.claimBuffer();
    }
    
    /**
     * @reason Reuse GL buffers and claim in batches - on some systems new buffers are expensive.
     * @author grondag
     */
    @Overwrite
    public static void glDeleteBuffers(int buffer)
    {
        GLBufferStore.releaseBuffer(buffer);
    }
}
