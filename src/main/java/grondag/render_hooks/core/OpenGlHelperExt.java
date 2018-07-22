package grondag.render_hooks.core;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferChecks;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.ProgramManager;
import net.minecraft.client.renderer.OpenGlHelper;

public class OpenGlHelperExt
{
    
    static private long glVertexAttribPointerFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglVertexAttribPointerBO = null;
    
    /**
     *  call after known that GL context is initialized
     */
    public static void initialize()
    {
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glVertexAttribPointer");
            pointer.setAccessible(true);
            glVertexAttribPointerFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glVertexAttribPointerFunctionPointer);
            
            nglVertexAttribPointerBO = GL20.class.getDeclaredMethod("nglVertexAttribPointerBO", int.class, int.class, int.class, boolean.class, int.class, long.class, long.class);
            nglVertexAttribPointerBO.setAccessible(true);
        }
        catch(Exception e)
        {
            glVertexAttribPointerFunctionPointer = -1;
            RenderHooks.INSTANCE.getLog().error("Unable to obtain a direct call path to glVertexAttribPointer due to reflection error. Render rerformance may be reduced when mod is enabled.", e);
        }
    }
    
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

    public static String getProgramInfoLog(int obj)
    {
        return OpenGlHelper.glGetProgramInfoLog(obj, OpenGlHelper.glGetProgrami(obj, GL20.GL_INFO_LOG_LENGTH));
    }

    public static String getShaderInfoLog(int obj)
    {
        return OpenGlHelper.glGetProgramInfoLog(obj, OpenGlHelper.glGetShaderi(obj, GL20.GL_INFO_LOG_LENGTH));
    }
    
    public static @Nullable ByteBuffer readFileAsString(String filename) throws Exception
    {
        InputStream in = ProgramManager.class.getResourceAsStream(filename);
    
        if(in == null)
            return null;
    
        byte[] abyte = IOUtils.toByteArray(new BufferedInputStream(in));
        ByteBuffer bytebuffer = BufferUtils.createByteBuffer(abyte.length);
        bytebuffer.put(abyte);
        bytebuffer.position(0);
        return bytebuffer;
    }

    public static int createShader(String filename, int shaderType)
    {
        int shader = 0;
        try
        {
            shader = OpenGlHelper.glCreateShader(shaderType);
    
            if(shader == 0)
                return 0;
    
            @Nullable ByteBuffer source = readFileAsString(filename);
            
            if(source == null)
                return 0;
            
            OpenGlHelper.glShaderSource(shader, source);
            OpenGlHelper.glCompileShader(shader);
    
            if (OpenGlHelper.glGetProgrami(shader, OpenGlHelper.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getProgramInfoLog(shader));
    
            return shader;
        }
        catch(Exception e)
        {
            OpenGlHelper.glDeleteShader(shader);
            RenderHooks.INSTANCE.getLog().error("Unable to create shader", e);
            return -1;
        }
    }
    
    private static int attributeEnabledCount = 0;
    
    /**
     * Using 1-based numbering for attribute slots because GL (on my machine at least) not liking slot 0
     */
    public static void enableAttributes(int enabledCount)
    {
        if(enabledCount > attributeEnabledCount)
        {
            while(enabledCount > attributeEnabledCount)
                GL20.glEnableVertexAttribArray(1 + attributeEnabledCount++);
        }
        else if(enabledCount < attributeEnabledCount)
        {
            while(enabledCount < attributeEnabledCount)
                GL20.glDisableVertexAttribArray(--attributeEnabledCount + 1);
        }
    }
    
    /**
     * Tries to circumvent the sanity checks LWJGL does because they significantly harm performance.
     * VBO will always been enabled when this is called and the function pointer checks are done 1X at init.
     */
    public static void glVertexAttribPointerFast(int index, int size, int type, boolean normalized, int stride, long buffer_buffer_offset)
    {
        if(glVertexAttribPointerFunctionPointer == -1)
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, buffer_buffer_offset);
        else
            try
            {
                nglVertexAttribPointerBO.invoke(null, index, size, type, normalized, stride, buffer_buffer_offset, glVertexAttribPointerFunctionPointer);
            }
            catch (Exception e)
            {
                RenderHooks.INSTANCE.getLog().error("Unable to obtain a direct call path to glVertexAttribPointer due to reflection error. Render rerformance may be reduced when mod is enabled.", e);
                glVertexAttribPointerFunctionPointer = -1;
                GL20.glVertexAttribPointer(index, size, type, normalized, stride, buffer_buffer_offset);
            }
    }

}
