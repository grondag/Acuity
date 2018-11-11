package grondag.acuity.opengl;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferChecks;
import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.APPLEFlushBufferRange;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Acuity;
import grondag.acuity.Configurator;
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
    static private MethodHandle nglVertexAttribPointerBO = null;
    
    static private long glTexCoordPointerFunctionPointer = -1;
    static private MethodHandle nglTexCoordPointerBO = null;
    
    static private long glVertexPointerFunctionPointer = -1;
    static private MethodHandle nglVertexPointerBO = null;

    static private long glColorPointerFunctionPointer = -1;
    static private MethodHandle nglColorPointerBO = null;
    
    static private long glClientActiveTextureFunctionPointer = -1;
    static private MethodHandle nglClientActiveTexturePointer = null;
    
    static private long glDrawArraysFunctionPointer = -1;
    static private MethodHandle nglDrawArrays = null;
    
    static private long glBindBufferFunctionPointer = -1;
    static private MethodHandle nglBindBuffer = null;
    
    static private long glUseProgramFunctionPointer = -1;
    static private MethodHandle nglUseProgram = null;
    
    static private boolean vaoEnabled = false;
    public static boolean isVaoEnabled()
    {
        return vaoEnabled && Configurator.enable_vao;
    }
    
    static private long glBindVertexArrayFunctionPointer = -1;
    static private MethodHandle nglBindVertexArray = null;
    
    static private long glGenVertexArraysFunctionPointer = -1;
    static private MethodHandle nglGenVertexArrays = null;
    
    static private long glDeleteVertexArraysFunctionPointer = -1;
    static private MethodHandle nglDeleteVertexArrays = null;
    
    static private long glPushMatrixFunctionPointer = -1;
    static private MethodHandle nglPushMatrix = null;
    
    static private long glPopMatrixFunctionPointer = -1;
    static private MethodHandle nglPopMatrix = null;
    
    static private long glTranslatefFunctionPointer = -1;
    static private MethodHandle nglTranslatef = null;
    
    static private long glMultMatrixfFunctionPointer = -1;
    static private MethodHandle nglMultMatrixf = null;
    
    static private long glUniformMatrix4fvFunctionPointer = -1;
    static private MethodHandle nglUniformMatrix4fv = null;
    
    static private MethodHandle nioCopyFromArray = null;
    static private MethodHandle nioCopyFromIntArray = null;
    static private boolean fastNioCopy = true;
    static private long nioFloatArrayBaseOffset;
    static private boolean nioFloatNeedsFlip;
    static private MethodHandle fastMatrixBufferCopyHandler;
    
    static private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    
    /**
     *  call after known that GL context is initialized
     */
    public static void initialize()
    {

        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            
            appleMapping = caps.GL_APPLE_flush_buffer_range;
            if(caps.OpenGL30 || caps.GL_ARB_map_buffer_range)
            {
                // prefer standard GL ranged buffer map if available
                appleMapping = false;
            }
            else if(!appleMapping)
            {
                asynchBufferMapEnabled = false;
            }
            
            if(caps.OpenGL30)
            {
                Field pointer = ContextCapabilities.class.getDeclaredField("glBindVertexArray");
                pointer.setAccessible(true);
                glBindVertexArrayFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glBindVertexArrayFunctionPointer);
                Method nglBindVertexArray = GL30.class.getDeclaredMethod("nglBindVertexArray", int.class, long.class);
                nglBindVertexArray.setAccessible(true);
                OpenGlHelperExt.nglBindVertexArray = lookup.unreflect(nglBindVertexArray);
                
                pointer = ContextCapabilities.class.getDeclaredField("glGenVertexArrays");
                pointer.setAccessible(true);
                glGenVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glGenVertexArraysFunctionPointer);
                Method nglGenVertexArrays = GL30.class.getDeclaredMethod("nglGenVertexArrays", int.class, long.class, long.class);
                nglGenVertexArrays.setAccessible(true);
                OpenGlHelperExt.nglGenVertexArrays = lookup.unreflect(nglGenVertexArrays);
                
                pointer = ContextCapabilities.class.getDeclaredField("glDeleteVertexArrays");
                pointer.setAccessible(true);
                glDeleteVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glDeleteVertexArraysFunctionPointer);
                Method nglDeleteVertexArrays = GL30.class.getDeclaredMethod("nglDeleteVertexArrays", int.class, long.class, long.class);
                nglDeleteVertexArrays.setAccessible(true);
                OpenGlHelperExt.nglDeleteVertexArrays = lookup.unreflect(nglDeleteVertexArrays);
                
                vaoEnabled = true;
            }
            else if(caps.GL_APPLE_vertex_array_object)
            {
                Field pointer = ContextCapabilities.class.getDeclaredField("glBindVertexArrayAPPLE");
                pointer.setAccessible(true);
                glBindVertexArrayFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glBindVertexArrayFunctionPointer);
                Method nglBindVertexArray = APPLEVertexArrayObject.class.getDeclaredMethod("nglBindVertexArrayAPPLE", int.class, long.class);
                nglBindVertexArray.setAccessible(true);
                OpenGlHelperExt.nglBindVertexArray = lookup.unreflect(nglBindVertexArray);
                
                pointer = ContextCapabilities.class.getDeclaredField("glGenVertexArraysAPPLE");
                pointer.setAccessible(true);
                glGenVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glGenVertexArraysFunctionPointer);
                Method nglGenVertexArrays = APPLEVertexArrayObject.class.getDeclaredMethod("nglGenVertexArraysAPPLE", int.class, long.class, long.class);
                nglGenVertexArrays.setAccessible(true);
                OpenGlHelperExt.nglGenVertexArrays = lookup.unreflect(nglGenVertexArrays);
                
                pointer = ContextCapabilities.class.getDeclaredField("glDeleteVertexArraysAPPLE");
                pointer.setAccessible(true);
                glDeleteVertexArraysFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glDeleteVertexArraysFunctionPointer);
                Method nglDeleteVertexArrays = APPLEVertexArrayObject.class.getDeclaredMethod("nglDeleteVertexArraysAPPLE", int.class, long.class, long.class);
                nglDeleteVertexArrays.setAccessible(true);
                OpenGlHelperExt.nglDeleteVertexArrays = lookup.unreflect(nglDeleteVertexArrays);
                
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
            Method nglVertexAttribPointerBO = GL20.class.getDeclaredMethod("nglVertexAttribPointerBO", int.class, int.class, int.class, boolean.class, int.class, long.class, long.class);
            nglVertexAttribPointerBO.setAccessible(true);
            OpenGlHelperExt.nglVertexAttribPointerBO = lookup.unreflect(nglVertexAttribPointerBO);
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
            Method nglTexCoordPointerBO = GL11.class.getDeclaredMethod("nglTexCoordPointerBO", int.class, int.class, int.class, long.class, long.class);
            nglTexCoordPointerBO.setAccessible(true);
            OpenGlHelperExt.nglTexCoordPointerBO = lookup.unreflect(nglTexCoordPointerBO);
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
            Method nglVertexPointerBO = GL11.class.getDeclaredMethod("nglVertexPointerBO", int.class, int.class, int.class, long.class, long.class);
            nglVertexPointerBO.setAccessible(true);
            OpenGlHelperExt.nglVertexPointerBO = lookup.unreflect(nglVertexPointerBO);
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
            Method nglColorPointerBO = GL11.class.getDeclaredMethod("nglColorPointerBO", int.class, int.class, int.class, long.class, long.class);
            nglColorPointerBO.setAccessible(true);
            OpenGlHelperExt.nglColorPointerBO = lookup.unreflect(nglColorPointerBO);
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
            Method nglClientActiveTexturePointer = GL13.class.getDeclaredMethod("nglClientActiveTexture", int.class, long.class);
            nglClientActiveTexturePointer.setAccessible(true);
            OpenGlHelperExt.nglClientActiveTexturePointer = lookup.unreflect(nglClientActiveTexturePointer);
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
            Method nglDrawArrays = GL11.class.getDeclaredMethod("nglDrawArrays", int.class, int.class, int.class, long.class);
            nglDrawArrays.setAccessible(true);
            OpenGlHelperExt.nglDrawArrays = lookup.unreflect(nglDrawArrays);
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
            Method nglBindBuffer = GL15.class.getDeclaredMethod("nglBindBuffer", int.class, int.class, long.class);
            nglBindBuffer.setAccessible(true);
            OpenGlHelperExt.nglBindBuffer = lookup.unreflect(nglBindBuffer);
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
            Method nglUseProgram = GL20.class.getDeclaredMethod("nglUseProgram", int.class, long.class);
            nglUseProgram.setAccessible(true);
            OpenGlHelperExt.nglUseProgram = lookup.unreflect(nglUseProgram);
        }
        catch(Exception e)
        {
            glUseProgramFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glUseProgram"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glPushMatrix");
            pointer.setAccessible(true);
            glPushMatrixFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glPushMatrixFunctionPointer);
            Method nglPushMatrix = GL11.class.getDeclaredMethod("nglPushMatrix", long.class);
            nglPushMatrix.setAccessible(true);
            OpenGlHelperExt.nglPushMatrix = lookup.unreflect(nglPushMatrix);
        }
        catch(Exception e)
        {
            glPushMatrixFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glPushMatrix"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glPopMatrix");
            pointer.setAccessible(true);
            glPopMatrixFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glPopMatrixFunctionPointer);
            Method nglPopMatrix = GL11.class.getDeclaredMethod("nglPopMatrix", long.class);
            nglPopMatrix.setAccessible(true);
            OpenGlHelperExt.nglPopMatrix = lookup.unreflect(nglPopMatrix);
        }
        catch(Exception e)
        {
            glUseProgramFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glPopMatrix"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glTranslatef");
            pointer.setAccessible(true);
            glTranslatefFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glTranslatefFunctionPointer);
            Method nglTranslatef = GL11.class.getDeclaredMethod("nglTranslatef", float.class, float.class, float.class, long.class);
            nglTranslatef.setAccessible(true);
            OpenGlHelperExt.nglTranslatef = lookup.unreflect(nglTranslatef);
        }
        catch(Exception e)
        {
            glTranslatefFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glTranslatef"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField("glMultMatrixf");
            pointer.setAccessible(true);
            glMultMatrixfFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(glMultMatrixfFunctionPointer);
            Method nglMultMatrixf = GL11.class.getDeclaredMethod("nglMultMatrixf", long.class, long.class);
            nglMultMatrixf.setAccessible(true);
            OpenGlHelperExt.nglMultMatrixf = lookup.unreflect(nglMultMatrixf);
        }
        catch(Exception e)
        {
            glMultMatrixfFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glMultMatrixf"), e);
        }
        
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            if(caps.OpenGL21)
            {
                Field pointer = ContextCapabilities.class.getDeclaredField("glUniformMatrix4fv");
                pointer.setAccessible(true);
                glUniformMatrix4fvFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glUniformMatrix4fvFunctionPointer);
                Method nglUniformMatrix4fv = GL20.class.getDeclaredMethod("nglUniformMatrix4fv", int.class, int.class, boolean.class, long.class, long.class);
                nglUniformMatrix4fv.setAccessible(true);
                OpenGlHelperExt.nglUniformMatrix4fv = lookup.unreflect(nglUniformMatrix4fv);
            }
            else 
            {
                Field pointer = ContextCapabilities.class.getDeclaredField("glUniformMatrix4fvARB");
                pointer.setAccessible(true);
                glUniformMatrix4fvFunctionPointer = pointer.getLong(caps);
                BufferChecks.checkFunctionAddress(glUniformMatrix4fvFunctionPointer);
                Method nglUniformMatrix4fv = ARBShaderObjects.class.getDeclaredMethod("nglUniformMatrix4fvARB", int.class, int.class, boolean.class, long.class, long.class);
                nglUniformMatrix4fv.setAccessible(true);
                OpenGlHelperExt.nglUniformMatrix4fv = lookup.unreflect(nglUniformMatrix4fv);
            }
        }
        catch(Exception e)
        {
            glUniformMatrix4fvFunctionPointer = -1;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glUniformMatrix4fv"), e);
        }
        
        try
        {
            Class<?> clazz = Class.forName("java.nio.Bits");
            Method nioCopyFromArray = clazz.getDeclaredMethod("copyFromArray", Object.class, long.class, long.class, long.class, long.class);
            nioCopyFromArray.setAccessible(true);
            OpenGlHelperExt.nioCopyFromArray = lookup.unreflect(nioCopyFromArray);
            
            Method nioCopyFromIntArray = clazz.getDeclaredMethod("copyFromIntArray", Object.class, long.class, long.class, long.class);
            nioCopyFromIntArray.setAccessible(true);
            OpenGlHelperExt.nioCopyFromIntArray = lookup.unreflect(nioCopyFromIntArray);
            
            clazz = Class.forName("java.nio.DirectFloatBufferU");
            Field f = clazz.getDeclaredField("arrayBaseOffset");
            f.setAccessible(true);
            nioFloatArrayBaseOffset = f.getLong(null);
            
            FloatBuffer testBuffer = BufferUtils.createFloatBuffer(16);
            nioFloatNeedsFlip = testBuffer.order() != ByteOrder.nativeOrder();
            
            fastNioCopy = true;
            
            if(fastNioCopy)
            {
                Method handlerMethod;
                if(nioFloatNeedsFlip)
                    handlerMethod = OpenGlHelperExt.class.getDeclaredMethod("fastMatrix4fBufferCopyFlipped", float[].class, long.class);
                else
                    handlerMethod = OpenGlHelperExt.class.getDeclaredMethod("fastMatrix4fBufferCopyStraight", float[].class, long.class);
                
                fastMatrixBufferCopyHandler = lookup.unreflect(handlerMethod);
            }
        }
        catch(Exception e)
        {
            fastNioCopy = false;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "fastNioCopy"), e);
        }
        
        glBufferDataFunctionPointer = functionPointer(OpenGlHelper.arbVbo ? "glBufferDataARB" : "glBufferData");
        nglBufferData = OpenGlHelper.arbVbo
                ? nativeMethod(ARBVertexBufferObject.class, "nglBufferDataARB", 
                        int.class, long.class, long.class, int.class, long.class)
                : nativeMethod(GL15.class, "nglBufferData", 
                        int.class, long.class, long.class, int.class, long.class);
        
        glmapBufferAsynchFunctionPointer = functionPointer(appleMapping ? "glMapBuffer" : "glMapBufferRange");
        nglmapBufferAsynch = appleMapping
                ? nativeMethod(GL15.class, "nglMapBuffer", 
                        int.class, int.class, long.class, ByteBuffer.class, long.class)
                : nativeMethod(GL30.class, "nglMapBufferRange", 
                        int.class, long.class, long.class, int.class, ByteBuffer.class, long.class);
                
        glBufferParameteriAPPLEFunctionPointer = functionPointer("glBufferParameteriAPPLE");
        nglBufferParameteriAPPLE = nativeMethod(APPLEFlushBufferRange.class, "nglBufferParameteriAPPLE", 
                int.class, int.class, int.class, long.class);
        
        glFlushMappedBufferRangeFunctionPointer = functionPointer(appleMapping ? "glFlushMappedBufferRangeAPPLE" : "glFlushMappedBufferRange");
        nglFlushMappedBufferRange = appleMapping
                ? nativeMethod(APPLEFlushBufferRange.class, "nglFlushMappedBufferRangeAPPLE", 
                        int.class, long.class, long.class, long.class)
                : nativeMethod(GL30.class, "nglFlushMappedBufferRange", 
                        int.class, long.class, long.class, long.class);
                
        glUnmapBufferFunctionPointer = functionPointer("glUnmapBuffer");
        nglUnmapBuffer = nativeMethod(GL15.class, "nglUnmapBuffer", 
                int.class, long.class);
    }
    
    public static void glGenVertexArrays(IntBuffer arrays)
    {
        if(vaoEnabled) try
        {
            nglGenVertexArrays.invokeExact(arrays.remaining(), MemoryUtil.getAddress(arrays), glGenVertexArraysFunctionPointer);
        }
        catch(Throwable e)
        {
            vaoEnabled = false;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "Vertex Array Objects"), e);
        }
    }
    
    public static void glBindVertexArray(int array)
    {
        if(vaoEnabled) try
        {
            nglBindVertexArray.invokeExact(array, glBindVertexArrayFunctionPointer);
        }
        catch(Throwable e)
        {
            vaoEnabled = false;
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "Vertex Array Objects"), e);
        }
    }
    
    public static void glDeleteVertexArrays(IntBuffer arrays)
    {
        if(vaoEnabled) try
        {
            nglDeleteVertexArrays.invokeExact(arrays.remaining(), MemoryUtil.getAddress(arrays), glDeleteVertexArraysFunctionPointer);
        }
        catch(Throwable e)
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
     * Enables the given number of generic vertex attributes if not already enabled.
     * Using 1-based numbering for attribute slots because GL (on my machine at least) not liking slot 0.
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
     * Disables all generic vertex attributes and resets tracking state.
     * Use after calling {@link #enableAttributesVao(int)}
     */
    public static void resetAttributes()
    {
        for(int i = 0; i < 6; i++)
        {
            GL20.glDisableVertexAttribArray(i);
        }
        attributeEnabledCount = 0;
    }
    
    /**
     * Like {@link #enableAttributes(int)} but enables all attributes 
     * regardless of prior state. Tracking state for {@link #enableAttributes(int)} remains unchanged.
     * Used to initialize VAO state
     */
    public static void enableAttributesVao(int enabledCount)
    {
        for(int i = 1; i <= enabledCount; i++)
        {
            GL20.glEnableVertexAttribArray(i);
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
                nglVertexAttribPointerBO.invokeExact(index, size, type, normalized, stride, buffer_buffer_offset, glVertexAttribPointerFunctionPointer);
            }
            catch (Throwable e)
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
                nglTexCoordPointerBO.invokeExact(size, type, stride, buffer_buffer_offset, glTexCoordPointerFunctionPointer);
            }
            catch (Throwable e)
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
                nglVertexPointerBO.invokeExact(size, type, stride, buffer_buffer_offset, glVertexPointerFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glVertexPointer"), e);
                glVertexPointerFunctionPointer = -1;
                GL11.glVertexPointer(size, type, stride, buffer_buffer_offset);
            }
    }
    
    public static void glColorPointerFast(int size, int type, int stride, long pointer_buffer_offset)
    {
        if(glColorPointerFunctionPointer == -1)
            GL11.glColorPointer(size, type, stride, pointer_buffer_offset);
        else
            try
            {
                nglColorPointerBO.invokeExact(size, type, stride, pointer_buffer_offset, glColorPointerFunctionPointer);
            }
            catch (Throwable e)
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
                nglClientActiveTexturePointer.invokeExact(textureId, glClientActiveTextureFunctionPointer);
            }
            catch (Throwable e)
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
                nglDrawArrays.invokeExact(mode, first, count, glDrawArraysFunctionPointer);
            }
            catch (Throwable e)
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
                nglBindBuffer.invokeExact(target, buffer, glBindBufferFunctionPointer);
            }
            catch (Throwable e)
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
                nglUseProgram.invokeExact(programId, glUseProgramFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glUseProgram"), e);
                glUseProgramFunctionPointer = -1;
                OpenGlHelper.glUseProgram(programId);
            }
    }
    
    public static void pushMatrixFast()
    {
        if(glPushMatrixFunctionPointer == -1)
            GL11.glPushMatrix();
        else
            try
            {
                nglPushMatrix.invokeExact(glPushMatrixFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glPushMatrix"), e);
                glPushMatrixFunctionPointer = -1;
                GL11.glPushMatrix();
            }
    }

    public static void popMatrixFast()
    {
        if(glPopMatrixFunctionPointer == -1)
            GL11.glPopMatrix();
        else
            try
            {
                nglPopMatrix.invokeExact(glPopMatrixFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glPopMatrix"), e);
                glPopMatrixFunctionPointer = -1;
                GL11.glPopMatrix();
            }
    }
    
    public static void translateFast(float x, float y, float z)
    {
        if(glTranslatefFunctionPointer == -1)
            GL11.glTranslatef(x, y, z);
        else
            try
            {
                nglTranslatef.invokeExact(x, y, z, glTranslatefFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glTranslatef"), e);
                glTranslatefFunctionPointer = -1;
                GL11.glTranslatef(x, y, z);
            }
    }
    
    public static void multMatrixFast(FloatBuffer matrix)
    {
        if(glMultMatrixfFunctionPointer == -1)
            GL11.glMultMatrix(matrix);
        else
            try
            {
                nglMultMatrixf.invokeExact(MemoryUtil.getAddress(matrix), glMultMatrixfFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glMultMatrixf"), e);
                glMultMatrixfFunctionPointer = -1;
                GL11.glMultMatrix(matrix);
            }
    }
    
    /**
     * Note that bufferAddress and matrix are redundant but both needed.  Obtain bufferAddress via lwjgl memory utility
     * and then cache it.  FloatBuffer will only be used if fast call doesn't work.
     */
    public static void glUniformMatrix4Fast(int location, boolean transpose, FloatBuffer matrix, long bufferAddress)
    {
        if(glUniformMatrix4fvFunctionPointer == -1)
            OpenGlHelper.glUniformMatrix4(location, transpose, matrix);
        else
            try
            {
                nglUniformMatrix4fv.invokeExact(location,  1, transpose, bufferAddress, glUniformMatrix4fvFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glUniformMatrix4fv"), e);
                glUniformMatrix4fvFunctionPointer = -1;
                OpenGlHelper.glUniformMatrix4(location, transpose, matrix);
            }
    }
    
    private static final float[] LOAD_ARRAY = new float[16];
    /**
     * NOT THREAD SAFE
     */
    public static void loadTransposeQuickly(FloatBuffer source, Matrix4f dest)
    {
        final float[] load = LOAD_ARRAY;
        source.get(load, 0, 16);
        dest.m00 = load[0];
        dest.m10 = load[1];
        dest.m20 = load[2];
        dest.m30 = load[3];
        dest.m01 = load[4];
        dest.m11 = load[5];
        dest.m21 = load[6];
        dest.m31 = load[7];
        dest.m02 = load[8];
        dest.m12 = load[9];
        dest.m22 = load[10];
        dest.m32 = load[11];
        dest.m03 = load[12];
        dest.m13 = load[13];
        dest.m23 = load[14];
        dest.m33 = load[15];
    }
    
    public static final boolean isFastNioCopyEnabled()
    {
        return fastNioCopy;
    }
    
    public static final void fastMatrix4fBufferCopy(float[] elements, long bufferAddress)
    {
        try
        {
            fastMatrixBufferCopyHandler.invokeExact(elements, bufferAddress);
        }
        catch (Throwable e)
        {
            throw new UnsupportedOperationException(e); 
        }
    }
    
    public static final void fastMatrix4fBufferCopyFlipped(float[] elements, long bufferAddress) throws Throwable
    {
        nioCopyFromIntArray.invokeExact((Object)elements, 0l, bufferAddress, 64l);
    }
    
    public static final void fastMatrix4fBufferCopyStraight(float[] elements, long bufferAddress) throws Throwable
    {
        nioCopyFromArray.invokeExact((Object)elements, nioFloatArrayBaseOffset, 0l, bufferAddress, 64l);
    }
    
    private static boolean appleMapping = false;
    private static boolean asynchBufferMapEnabled = true;
    public static boolean areAsynchMappedBuffersSupported()
    {
        return asynchBufferMapEnabled;
    }
    
    static private long glBufferDataFunctionPointer = -1;
    static private MethodHandle nglBufferData = null;
    public static void glBufferData(int target, int size, int usage)
    {
        if(glBufferDataFunctionPointer == -1)
            glBufferDataSlow(target, size, usage);
        else
            try
            {
                nglBufferData.invokeExact(target, (long)size, 0L, usage, glBufferDataFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glBufferData"), e);
                glBufferDataFunctionPointer = -1;
                glBufferDataSlow(target, size, usage);
            }
    }

    private static void glBufferDataSlow(int target, int size, int usage)
    {
        if (OpenGlHelper.arbVbo)
        {
            ARBVertexBufferObject.glBufferDataARB(target, size, usage);
        }
        else
        {
            GL15.glBufferData(target, size, usage);
        }
    }
    
    static private long glmapBufferAsynchFunctionPointer = -1;
    static private MethodHandle nglmapBufferAsynch = null;
    /** 
     * Assumes buffer is bound and starting offset is 0. 
     * Maps whole buffer. (Size should be size of buffer.)
     * If writeFlag true, buffer mapped for writing. If false, mapped for reading.
     */
    public static @Nullable ByteBuffer mapBufferAsynch(@Nullable ByteBuffer priorMapped, int bufferSize, boolean writeFlag)
    {
        if(glmapBufferAsynchFunctionPointer == -1)
            return mapBufferAsynchSlow(priorMapped, bufferSize, writeFlag);
        else
            try
            {
                ByteBuffer result;
                
                if(appleMapping || !writeFlag)
                    result = (ByteBuffer) nglmapBufferAsynch.invokeExact(
                            OpenGlHelper.GL_ARRAY_BUFFER, 
                            writeFlag ? GL15.GL_WRITE_ONLY : GL15.GL_READ_ONLY, 
                            (long)bufferSize, priorMapped, 
                            glmapBufferAsynchFunctionPointer);
                else
                    result = (ByteBuffer) nglmapBufferAsynch.invokeExact(
                            OpenGlHelper.GL_ARRAY_BUFFER, 
                            0L, 
                            (long)bufferSize, 
                            GL30.GL_MAP_FLUSH_EXPLICIT_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT | GL30.GL_MAP_WRITE_BIT, 
                            priorMapped, 
                            glmapBufferAsynchFunctionPointer);
                
                if(result != null)
                    result.order(ByteOrder.nativeOrder());
                
                return result;
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "mapBufferAsynch"), e);
                glmapBufferAsynchFunctionPointer = -1;
                return mapBufferAsynchSlow(priorMapped, bufferSize, writeFlag);
            }
        
    }
    
    private static @Nullable ByteBuffer mapBufferAsynchSlow(@Nullable ByteBuffer priorMapped, int bufferSize, boolean writeFlag)
    {
        if(appleMapping)
        {
            return GL15.glMapBuffer(
                    OpenGlHelper.GL_ARRAY_BUFFER, 
                    writeFlag ? GL15.GL_WRITE_ONLY : GL15.GL_READ_ONLY, 
                    bufferSize, 
                    priorMapped);
        }
        else
        {
            return GL30.glMapBufferRange(OpenGlHelper.GL_ARRAY_BUFFER, 0, bufferSize, 
                    GL30.GL_MAP_FLUSH_EXPLICIT_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT | GL30.GL_MAP_WRITE_BIT,
                    priorMapped);
        }
    }
    
    static private long glBufferParameteriAPPLEFunctionPointer = -1;
    static private MethodHandle nglBufferParameteriAPPLE = null;
    /**
     * Call on buffers that will be mapped and should be unsynchronized / explicitly flushed.
     * Has no effect with non-apple drivers but is essential for mapped buffers with Apple drivers
     * that do not support standard buffer range mapping.
     */
    public static void handleAppleMappedBuffer()
    {
        if(appleMapping)
        {
            if(glBufferParameteriAPPLEFunctionPointer == -1)
                handleAppleMappedBufferSlow();
            else
                try
                {
                    nglBufferParameteriAPPLE.invokeExact(OpenGlHelper.GL_ARRAY_BUFFER, APPLEFlushBufferRange.GL_BUFFER_SERIALIZED_MODIFY_APPLE, GL11.GL_FALSE, glBufferParameteriAPPLEFunctionPointer);
                    nglBufferParameteriAPPLE.invokeExact(OpenGlHelper.GL_ARRAY_BUFFER, APPLEFlushBufferRange.GL_BUFFER_FLUSHING_UNMAP_APPLE, GL11.GL_FALSE, glBufferParameteriAPPLEFunctionPointer);
                }
                catch (Throwable e)
                {
                    Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "handleAppleMappedBuffer"), e);
                    glBufferParameteriAPPLEFunctionPointer = -1;
                    handleAppleMappedBufferSlow();
                }
        }
    }

    private static void handleAppleMappedBufferSlow()
    {
        APPLEFlushBufferRange.glBufferParameteriAPPLE(OpenGlHelper.GL_ARRAY_BUFFER, APPLEFlushBufferRange.GL_BUFFER_SERIALIZED_MODIFY_APPLE, GL11.GL_FALSE);
        APPLEFlushBufferRange.glBufferParameteriAPPLE(OpenGlHelper.GL_ARRAY_BUFFER, APPLEFlushBufferRange.GL_BUFFER_FLUSHING_UNMAP_APPLE, GL11.GL_FALSE);

    }

    static private long glFlushMappedBufferRangeFunctionPointer = -1;
    static private MethodHandle nglFlushMappedBufferRange = null;
    
    static private long glUnmapBufferFunctionPointer = -1;
    static private MethodHandle nglUnmapBuffer = null;
    
    public static void flushBuffer(long offset, long length)
    {
        if(glFlushMappedBufferRangeFunctionPointer == -1)
        {
            flushBufferSlow(offset, length);
        }
        else 
            try
            {
                nglFlushMappedBufferRange.invokeExact(OpenGlHelper.GL_ARRAY_BUFFER, offset, length, glFlushMappedBufferRangeFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "flushBuffer"), e);
                glFlushMappedBufferRangeFunctionPointer = -1;
                flushBufferSlow(offset, length);
            }
    }
    
    private static void flushBufferSlow(long offset, long length)
    {
        if(appleMapping)
            APPLEFlushBufferRange.glFlushMappedBufferRangeAPPLE(OpenGlHelper.GL_ARRAY_BUFFER, offset, length);
        else
            GL30.glFlushMappedBufferRange(OpenGlHelper.GL_ARRAY_BUFFER, offset, length);
    }
    
    public static void unmapBuffer()
    {
        if(glUnmapBufferFunctionPointer == -1)
            unmapBufferSlow();
        else 
            try
            {
                @SuppressWarnings("unused")
                boolean discard = (boolean) nglUnmapBuffer.invokeExact(OpenGlHelper.GL_ARRAY_BUFFER, glUnmapBufferFunctionPointer);
            }
            catch (Throwable e)
            {
                Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "unmapBuffer"), e);
                glUnmapBufferFunctionPointer = -1;
                unmapBufferSlow();
            }
    }
    
    private static void unmapBufferSlow()
    {
        GL15.glUnmapBuffer(OpenGlHelper.GL_ARRAY_BUFFER);
    }
    
    private static long functionPointer(String capabilityFieldName)
    {
        try
        {
            ContextCapabilities caps = GLContext.getCapabilities();
            Field pointer = ContextCapabilities.class.getDeclaredField(capabilityFieldName);
            pointer.setAccessible(true);
            long result = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(result);
            return result;
        }
        catch(Exception e)
        {
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", capabilityFieldName), e);
            return -1;
        }
    }
    
    private static MethodHandle nativeMethod(Class<?> glClass, String methodName, Class<?>... args)
    {
        try
        {
            Method nativeMethod = glClass.getDeclaredMethod(methodName, args);
            nativeMethod.setAccessible(true);
            return lookup.unreflect(nativeMethod);
        }
        catch(Exception e)
        {
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", methodName), e);
            return null;
        }
    }
    
    public static boolean assertNoGLError(String marker)
    {
        int i = GlStateManager.glGetError();

        if (i == 0)
            return true;
        else
        {
            String s = GLU.gluErrorString(i);
            final Logger log = Acuity.INSTANCE.getLog();
            log.error("########## GL ERROR ##########");
            log.error("@ {}", (Object)marker);
            log.error("{}: {}", Integer.valueOf(i), s);
            return false;
        }
    }
}
