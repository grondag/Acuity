package grondag.acuity.mixin;

import java.nio.FloatBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.acuity.opengl.OpenGlHelperExt;
import net.minecraft.client.renderer.GlStateManager;

@Mixin(GlStateManager.class)
public class MixinGlStateManager
{
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void glDrawArrays(int mode, int first, int count)
    {
        OpenGlHelperExt.glDrawArraysFast(mode, first, count);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void popMatrix()
    {
        OpenGlHelperExt.popMatrixFast();
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void pushMatrix()
    {
        OpenGlHelperExt.pushMatrixFast();
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void translate(float x, float y, float z)
    {
        OpenGlHelperExt.translateFast(x, y, z);
    }

    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void translate(double x, double y, double z)
    {
        OpenGlHelperExt.translateFastDouble(x, y, z);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void multMatrix(FloatBuffer matrix)
    {
        OpenGlHelperExt.multMatrixFast(matrix);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void glColorPointer(int size, int type, int stride, int buffer_offset)
    {
        OpenGlHelperExt.glColorPointerFast(size, type, stride, (long)buffer_offset);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void glVertexPointer(int size, int type, int stride, int buffer_offset)
    {
        OpenGlHelperExt.glVertexPointerFast(size, type, stride, (long)buffer_offset);
    }
    
    /**
     * @reason Use native call for speed.
     * @author grondag
     */
    @Overwrite
    public static void glTexCoordPointer(int size, int type, int stride, int buffer_offset)
    {
        OpenGlHelperExt.glTexCoordPointerFast(size, type, stride, (long)buffer_offset);
    }
}
