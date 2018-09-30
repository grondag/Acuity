package grondag.acuity.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.lwjgl.BufferChecks;
import org.lwjgl.opengl.APPLEFence;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import grondag.acuity.Acuity;
import net.minecraft.util.text.translation.I18n;

public class OpenGlFenceExt
{
    public static abstract class Fence
    {
        public abstract boolean isReached();
       
        public abstract void set();
    }
    
    private static class AppleFence extends Fence
    {
        int fencePointer;
        
        private AppleFence()
        {
            fencePointer = APPLEFence.glGenFencesAPPLE();
        }
        
        @Override
        public final boolean isReached()
        {
            return APPLEFence.glTestFenceAPPLE(fencePointer);
        }

        @Override
        public final void set()
        {
            APPLEFence.glSetFenceAPPLE(fencePointer);
        }

        @Override
        protected void finalize() throws Throwable
        {
            APPLEFence.glDeleteFencesAPPLE(fencePointer); 
        }
        
    }
    
    static private long deleteFenceFunctionPointer = -1;
    @SuppressWarnings("null")
    static private MethodHandle deleteFenceHandle = null;
    
    static private long testFenceFunctionPointer = -1;
    @SuppressWarnings("null")
    static private MethodHandle testFenceHandle = null;
    
    static private long setFenceFunctionPointer = -1;
    @SuppressWarnings("null")
    static private MethodHandle setFenceHandle = null;
    
    static private long genFenceFunctionPointer = -1;
    @SuppressWarnings("null")
    static private MethodHandle genFenceHandle = null;
    
//    public static void initialize()
//    {
//        final MethodHandles.Lookup lookup = MethodHandles.lookup();
//
//        try
//        {
//            ContextCapabilities caps = GLContext.getCapabilities();
//            if(caps.OpenGL30)
//            {
//                Field pointer = ContextCapabilities.class.getDeclaredField("glBindVertexArray");
//                pointer.setAccessible(true);
//                glBindVertexArrayFunctionPointer = pointer.getLong(caps);
//                BufferChecks.checkFunctionAddress(glBindVertexArrayFunctionPointer);
//                Method nglBindVertexArray = GL30.class.getDeclaredMethod("nglBindVertexArray", int.class, long.class);
//                nglBindVertexArray.setAccessible(true);
//                OpenGlHelperExt.nglBindVertexArray = lookup.unreflect(nglBindVertexArray);
//                
//                pointer = ContextCapabilities.class.getDeclaredField("glGenVertexArrays");
//                pointer.setAccessible(true);
//                glGenVertexArraysFunctionPointer = pointer.getLong(caps);
//                BufferChecks.checkFunctionAddress(glGenVertexArraysFunctionPointer);
//                Method nglGenVertexArrays = GL30.class.getDeclaredMethod("nglGenVertexArrays", int.class, long.class, long.class);
//                nglGenVertexArrays.setAccessible(true);
//                OpenGlHelperExt.nglGenVertexArrays = lookup.unreflect(nglGenVertexArrays);
//                
//                pointer = ContextCapabilities.class.getDeclaredField("glDeleteVertexArrays");
//                pointer.setAccessible(true);
//                glDeleteVertexArraysFunctionPointer = pointer.getLong(caps);
//                BufferChecks.checkFunctionAddress(glDeleteVertexArraysFunctionPointer);
//                Method nglDeleteVertexArrays = GL30.class.getDeclaredMethod("nglDeleteVertexArrays", int.class, long.class, long.class);
//                nglDeleteVertexArrays.setAccessible(true);
//                OpenGlHelperExt.nglDeleteVertexArrays = lookup.unreflect(nglDeleteVertexArrays);
//                
//                vaoEnabled = true;
//            }
//            else if(caps.GL_APPLE_vertex_array_object)
//            {
//                Field pointer = ContextCapabilities.class.getDeclaredField("glBindVertexArrayAPPLE");
//                pointer.setAccessible(true);
//                glBindVertexArrayFunctionPointer = pointer.getLong(caps);
//                BufferChecks.checkFunctionAddress(glBindVertexArrayFunctionPointer);
//                Method nglBindVertexArray = APPLEVertexArrayObject.class.getDeclaredMethod("nglBindVertexArrayAPPLE", int.class, long.class);
//                nglBindVertexArray.setAccessible(true);
//                OpenGlHelperExt.nglBindVertexArray = lookup.unreflect(nglBindVertexArray);
//                
//                pointer = ContextCapabilities.class.getDeclaredField("glGenVertexArraysAPPLE");
//                pointer.setAccessible(true);
//                glGenVertexArraysFunctionPointer = pointer.getLong(caps);
//                BufferChecks.checkFunctionAddress(glGenVertexArraysFunctionPointer);
//                Method nglGenVertexArrays = APPLEVertexArrayObject.class.getDeclaredMethod("nglGenVertexArraysAPPLE", int.class, long.class, long.class);
//                nglGenVertexArrays.setAccessible(true);
//                OpenGlHelperExt.nglGenVertexArrays = lookup.unreflect(nglGenVertexArrays);
//                
//                pointer = ContextCapabilities.class.getDeclaredField("glDeleteVertexArraysAPPLE");
//                pointer.setAccessible(true);
//                glDeleteVertexArraysFunctionPointer = pointer.getLong(caps);
//                BufferChecks.checkFunctionAddress(glDeleteVertexArraysFunctionPointer);
//                Method nglDeleteVertexArrays = APPLEVertexArrayObject.class.getDeclaredMethod("nglDeleteVertexArraysAPPLE", int.class, long.class, long.class);
//                nglDeleteVertexArrays.setAccessible(true);
//                OpenGlHelperExt.nglDeleteVertexArrays = lookup.unreflect(nglDeleteVertexArrays);
//                
//                vaoEnabled = true;
//            }
//            else
//            {
//                vaoEnabled = false;  // for clarity - was already false
//                return;
//            }
//        }
//        catch(Exception e)
//        {
//            vaoEnabled = false;
//            glBindVertexArrayFunctionPointer = -1;
//            glDeleteVertexArraysFunctionPointer = -1;
//            glGenVertexArraysFunctionPointer = -1;
//            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "Vertex Array Objects"), e);
//        }
//    }
//    public static void deleteFence(int glFenceId)
//    {
//        APPLEFence.glDeleteFencesAPPLE(glFenceId);   
//        
//        try
//        {
//            nglTexCoordPointerBO.invokeExact(size, type, stride, buffer_buffer_offset, glTexCoordPointerFunctionPointer);
//        }
//        catch (Throwable e)
//        {
//            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glTexCoordPointer"), e);
//            glTexCoordPointerFunctionPointer = -1;
//            GL11.glTexCoordPointer(size, type, stride, buffer_buffer_offset);
//        }
//    }

    public static Fence create()
    {
        return new AppleFence();
    }
    
}
