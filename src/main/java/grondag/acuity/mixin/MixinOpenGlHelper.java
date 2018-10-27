package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.acuity.hooks.PipelineHooks;
import grondag.acuity.opengl.GLBufferStore;
import net.minecraft.client.renderer.OpenGlHelper;

@Mixin(OpenGlHelper.class)
public abstract class MixinOpenGlHelper
{
    @Overwrite
    public static boolean useVbo()
    {
        return PipelineHooks.useVbo();
    }

    @Overwrite
    public static int glGenBuffers()
    {
        return GLBufferStore.glGenBuffers();
    }
    
    @Overwrite
    public static void glDeleteBuffers(int buffer)
    {
        GLBufferStore.glDeleteBuffers(buffer);
    }
}
