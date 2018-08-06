package grondag.acuity.core;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferChecks;
import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import grondag.acuity.Acuity;
import grondag.acuity.api.PipelineManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OpenGlHelperExt
{
    
    static private long glVertexAttribPointerFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglVertexAttribPointerBO = null;
    
    static private long glTexCoordPointerFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglTexCoordPointerBO = null;
    
    static private long glVertexPointerFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglVertexPointerBO = null;

    static private long glColorPointerFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglColorPointerBO = null;
    
    static private long glClientActiveTextureFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglClientActiveTexturePointer = null;
    
    static private long glDrawArraysFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglDrawArrays = null;
    
    static private long glBindBufferFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglBindBuffer = null;
    
    static private long glUseProgramFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglUseProgram = null;
    
    static private boolean vaoEnabled = false;
    public static boolean isVaoEnabled()
    {
        return vaoEnabled;
    }
    
    static private long glBindVertexArrayFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglBindVertexArray = null;
    
    static private long glGenVertexArraysFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglGenVertexArrays = null;
    
    static private long glDeleteVertexArraysFunctionPointer = -1;
    @SuppressWarnings("null")
    static private Method nglDeleteVertexArrays = null;
    
    /**
     *  call after known that GL context is initialized
     */
    public static void initialize()
    {
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            if(caps.OpenGL30)
            {
                Field pointer = ContextCapabilities.class.getDeclaredField("glBindVertexArray");
                pointer.setAccessible(true);
                glBindVertexArrayFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glBindVertexArrayFunctionPointer);
                nglBindVertexArray = GL30.class.getDeclaredMethod("nglBindVertexArray", int.class, long.class);
                nglBindVertexArray.setAccessible(true);
                
                pointer = ContextCapabilities.class.getDeclaredField("glGenVertexArrays");
                pointer.setAccessible(true);
                glGenVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glGenVertexArraysFunctionPointer);
                nglGenVertexArrays = GL30.class.getDeclaredMethod("nglGenVertexArrays", int.class, long.class, long.class);
                nglGenVertexArrays.setAccessible(true);
                
                pointer = ContextCapabilities.class.getDeclaredField("glDeleteVertexArrays");
                pointer.setAccessible(true);
                glDeleteVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glDeleteVertexArraysFunctionPointer);
                nglDeleteVertexArrays = GL30.class.getDeclaredMethod("nglDeleteVertexArrays", int.class, long.class, long.class);
                nglDeleteVertexArrays.setAccessible(true);
                
                vaoEnabled = true;
            }
            else if(caps.GL_APPLE_vertex_array_object)
            {
                Field pointer = ContextCapabilities.class.getDeclaredField("glBindVertexArrayAPPLE");
                pointer.setAccessible(true);
                glBindVertexArrayFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glBindVertexArrayFunctionPointer);
                nglBindVertexArray = APPLEVertexArrayObject.class.getDeclaredMethod("nglBindVertexArrayAPPLE", int.class, long.class);
                nglBindVertexArray.setAccessible(true);
                
                pointer = ContextCapabilities.class.getDeclaredField("glGenVertexArraysAPPLE");
                pointer.setAccessible(true);
                glGenVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glGenVertexArraysFunctionPointer);
                nglGenVertexArrays = APPLEVertexArrayObject.class.getDeclaredMethod("nglGenVertexArraysAPPLE", int.class, long.class, long.class);
                nglGenVertexArrays.setAccessible(true);
                
                pointer = ContextCapabilities.class.getDeclaredField("glDeleteVertexArraysAPPLE");
                pointer.setAccessible(true);
                glDeleteVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glDeleteVertexArraysFunctionPointer);
                nglDeleteVertexArrays = APPLEVertexArrayObject.class.getDeclaredMethod("nglDeleteVertexArraysAPPLE", int.class, long.class, long.class);
                nglDeleteVertexArrays.setAccessible(true);
                
                vaoEnabled = true;
            }
            else
            {
                vaoEnabled = false;  // for clarity - was already false
                return;
            }
        }
        catch(Exception e)
        {
            vaoEnabled = false;
            glBindVertexArrayFunctionPointer = -1;
            glDeleteVertexArraysFunctionPointer = -1;
            glGenVertexArraysFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "Vertex Array Objects"), e);
        }
        
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
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glVertexAttribPointer"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glTexCoordPointer");
            pointer.setAccessible(true);
            glTexCoordPointerFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glTexCoordPointerFunctionPointer);
            nglTexCoordPointerBO = GL11.class.getDeclaredMethod("nglTexCoordPointerBO", int.class, int.class, int.class, long.class, long.class);
            nglTexCoordPointerBO.setAccessible(true);
        }
        catch(Exception e)
        {
            glTexCoordPointerFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glVertexAttribPointer"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glVertexPointer");
            pointer.setAccessible(true);
            glVertexPointerFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glVertexPointerFunctionPointer);
            nglVertexPointerBO = GL11.class.getDeclaredMethod("nglVertexPointerBO", int.class, int.class, int.class, long.class, long.class);
            nglVertexPointerBO.setAccessible(true);
        }
        catch(Exception e)
        {
            glVertexPointerFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glVertexPointer"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glColorPointer");
            pointer.setAccessible(true);
            glColorPointerFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glColorPointerFunctionPointer);
            nglColorPointerBO = GL11.class.getDeclaredMethod("nglColorPointerBO", int.class, int.class, int.class, long.class, long.class);
            nglColorPointerBO.setAccessible(true);
        }
        catch(Exception e)
        {
            glColorPointerFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glColorPointer"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glClientActiveTexture");
            pointer.setAccessible(true);
            glClientActiveTextureFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glClientActiveTextureFunctionPointer);
            nglClientActiveTexturePointer = GL13.class.getDeclaredMethod("nglClientActiveTexture", int.class, long.class);
            nglClientActiveTexturePointer.setAccessible(true);
        }
        catch(Exception e)
        {
            glClientActiveTextureFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glClientActiveTexture"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glDrawArrays");
            pointer.setAccessible(true);
            glDrawArraysFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glDrawArraysFunctionPointer);
            nglDrawArrays = GL11.class.getDeclaredMethod("nglDrawArrays", int.class, int.class, int.class, long.class);
            nglDrawArrays.setAccessible(true);
        }
        catch(Exception e)
        {
            glDrawArraysFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glDrawArrays"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glBindBuffer");
            pointer.setAccessible(true);
            glBindBufferFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glBindBufferFunctionPointer);
            nglBindBuffer = GL15.class.getDeclaredMethod("nglBindBuffer", int.class, int.class, long.class);
            nglBindBuffer.setAccessible(true);
        }
        catch(Exception e)
        {
            glBindBufferFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glBindBuffer"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glUseProgram");
            pointer.setAccessible(true);
            glUseProgramFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glUseProgramFunctionPointer);
            nglUseProgram = GL20.class.getDeclaredMethod("nglUseProgram", int.class, long.class);
            nglUseProgram.setAccessible(true);
        }
        catch(Exception e)
        {
            glUseProgramFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glUseProgram"), e);
        }
    }
    
    public static void glGenVertexArrays(IntBuffer arrays)
    {
        if(vaoEnabled) try
        {
            nglGenVertexArrays.invoke(null, arrays.remaining(), MemoryUtil.getAddress(arrays), glGenVertexArraysFunctionPointer);
        }
        catch(Exception e)
        {
            vaoEnabled = false;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "Vertex Array Objects"), e);
        }
    }
    
    public static void glBindVertexArray(int array)
    {
        if(vaoEnabled) try
        {
            nglBindVertexArray.invoke(null, array, glBindVertexArrayFunctionPointer);
        }
        catch(Exception e)
        {
            vaoEnabled = false;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "Vertex Array Objects"), e);
        }
    }
    
    public static void glDeleteVertexArrays(IntBuffer arrays)
    {
        if(vaoEnabled) try
        {
            nglDeleteVertexArrays.invoke(null, arrays.remaining(), MemoryUtil.getAddress(arrays), glDeleteVertexArraysFunctionPointer);
        }
        catch(Exception e)
        {
            vaoEnabled = false;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "Vertex Array Objects"), e);
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
        InputStream in = PipelineManager.class.getResourceAsStream(filename);
    
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
                throw new RuntimeException(getProgramInfoLog(shader));
    
            return shader;
        }
        catch(Exception e)
        {
            OpenGlHelper.glDeleteShader(shader);
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.fail_create_shader", filename, e.getMessage()), e);
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
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glVertexAttribPointer"), e);
                glVertexAttribPointerFunctionPointer = -1;
                GL20.glVertexAttribPointer(index, size, type, normalized, stride, buffer_buffer_offset);
            }
    }
    
    /**
     * Tries to circumvent the sanity checks LWJGL does because they significantly harm performance.
     * VBO will always been enabled when this is called and the function pointer checks are done 1X at init.
     */
    public static void glTexCoordPointerFast(int size, int type, int stride, long buffer_buffer_offset)
    {
        if(glTexCoordPointerFunctionPointer == -1)
            GL11.glTexCoordPointer(size, type, stride, buffer_buffer_offset);
        else
            try
            {
                nglTexCoordPointerBO.invoke(null, size, type, stride, buffer_buffer_offset, glTexCoordPointerFunctionPointer);
            }
            catch (Exception e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glTexCoordPointer"), e);
                glTexCoordPointerFunctionPointer = -1;
                GL11.glTexCoordPointer(size, type, stride, buffer_buffer_offset);
            }
    }

    /**
     * Tries to circumvent the sanity checks LWJGL does because they significantly harm performance.
     * VBO will always been enabled when this is called and the function pointer checks are done 1X at init.
     */
    public static void glVertexPointerFast(int size, int type, int stride, long buffer_buffer_offset)
    {
        if(glVertexPointerFunctionPointer == -1)
            GL11.glVertexPointer(size, type, stride, buffer_buffer_offset);
        else
            try
            {
                nglVertexPointerBO.invoke(null, size, type, stride, buffer_buffer_offset, glVertexPointerFunctionPointer);
            }
            catch (Exception e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glVertexPointer"), e);
                glVertexPointerFunctionPointer = -1;
                GL11.glVertexPointer(size, type, stride, buffer_buffer_offset);
            }
    }
    
    public static void glColorPointerFast(int size, int type, int stride, int pointer_buffer_offset)
    {
        if(glColorPointerFunctionPointer == -1)
            GL11.glColorPointer(size, type, stride, pointer_buffer_offset);
        else
            try
            {
                nglColorPointerBO.invoke(null, size, type, stride, pointer_buffer_offset, glColorPointerFunctionPointer);
            }
            catch (Exception e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glColorPointer"), e);
                glColorPointerFunctionPointer = -1;
                GL11.glColorPointer(size, type, stride, pointer_buffer_offset);
            }
    }

    public static void setClientActiveTextureFast(int textureId)
    {
        if(glClientActiveTextureFunctionPointer == -1)
            OpenGlHelper.setClientActiveTexture(textureId);
        else
            try
            {
                nglClientActiveTexturePointer.invoke(null, textureId, glClientActiveTextureFunctionPointer);
            }
            catch (Exception e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glClientActiveTexture"), e);
                glClientActiveTextureFunctionPointer = -1;
                OpenGlHelper.setClientActiveTexture(textureId);
            }
    }
    
    public static void glDrawArraysFast(int mode, int first, int count)
    {
        if(glDrawArraysFunctionPointer == -1)
            GlStateManager.glDrawArrays(mode, first, count);
        else
            try
            {
                nglDrawArrays.invoke(null, mode, first, count, glDrawArraysFunctionPointer);
            }
            catch (Exception e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glDrawArrays"), e);
                glDrawArraysFunctionPointer = -1;
                GlStateManager.glDrawArrays(mode, first, count);
            }
    }
    
    public static void glBindBufferFast(int target, int buffer)
    {
        if(glBindBufferFunctionPointer == -1)
            OpenGlHelper.glBindBuffer(target, buffer);
        else
            try
            {
                nglBindBuffer.invoke(null, target, buffer, glBindBufferFunctionPointer);
            }
            catch (Exception e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glBindBuffer"), e);
                glBindBufferFunctionPointer = -1;
                OpenGlHelper.glBindBuffer(target, buffer);
            }
    }
    
    public static void glUseProgramFast(int programId)
    {
        if(glUseProgramFunctionPointer == -1)
            OpenGlHelper.glUseProgram(programId);
        else
            try
            {
                nglUseProgram.invoke(null, programId, glUseProgramFunctionPointer);
            }
            catch (Exception e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glUseProgram"), e);
                glUseProgramFunctionPointer = -1;
                OpenGlHelper.glUseProgram(programId);
            }
    }
}
