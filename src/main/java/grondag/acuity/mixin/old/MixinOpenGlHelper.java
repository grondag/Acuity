package grondag.acuity.mixin.old;

import java.nio.FloatBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.acuity.hooks.PipelineHooks;
import grondag.acuity.opengl.GLBufferStore;
import grondag.acuity.opengl.OpenGlHelperExt;

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
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void glBindBuffer(int target, int buffer)
    {
        OpenGlHelperExt.glBindBufferFast(target, buffer);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void setClientActiveTexture(int texture)
    {
        OpenGlHelperExt.setClientActiveTextureFast(texture);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void glUseProgram(int program)
    {
        OpenGlHelperExt.glUseProgramFast(program);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void glUniformMatrix4(int location, boolean transpose, FloatBuffer matrices)
    {
        OpenGlHelperExt.glUniformMatrix4Fast(location, transpose, matrices, MemoryUtil.getAddress(matrices));
    }
}
