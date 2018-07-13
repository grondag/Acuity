package grondag.render_hooks.core;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL15;

import net.minecraft.client.renderer.OpenGlHelper;

public class OpenGlHelperExt
{
    /**
     * Allocates buffer of given size without uploading.
     */
    public static void glBufferData(int target, long data_size, int usage)
    {
        if (OpenGlHelper.arbVbo)
        {
            ARBVertexBufferObject.glBufferDataARB(target, data_size, usage);
        }
        else
        {
            GL15.glBufferData(target, data_size, usage);
        }
    }
    
    public static void glBufferSubData(int target, long offset, ByteBuffer data)
    {
        if (OpenGlHelper.arbVbo)
        {
            ARBVertexBufferObject.glBufferSubDataARB(target, offset, data);
        }
        else
        {
            GL15.glBufferSubData(target, offset, data);
        }
    }
}
