package grondag.render_hooks.api;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.core.OpenGlHelperExt;
import net.minecraft.client.renderer.OpenGlHelper;

abstract class AbstractPipelineShader
{
    public final String fileName;
    
    private final int shaderType;
    
    private int glId = -1;
    private boolean needsLoad = true;
    
    AbstractPipelineShader(@Nonnull String fileName, int shaderType)
    {
        this.fileName = fileName;
        this.shaderType = shaderType;
    }
    
    /**
     * Call after render / resource refresh to force shader reload.
     */
    public final void forceReload()
    {
        this.needsLoad = true;
    }
    
    public final int glId()
    {
        if(this.needsLoad)
            this.load();
        
        return this.glId;
    }
    
    private final void load()
    {
        this.needsLoad = false;
        try
        {
            if(this.glId <= 0)
            {
                this.glId = OpenGlHelper.glCreateShader(shaderType);
                if(this.glId == 0) 
                {
                    this.glId = -1;
                    return;
                }
            }
    
            @Nullable ByteBuffer source = OpenGlHelperExt.readFileAsString(this.fileName);
            
            if(source == null)
            {
                this.glId = -1;
                return;
            }
            
            OpenGlHelper.glShaderSource(this.glId, source);
            OpenGlHelper.glCompileShader(this.glId);
    
            if (OpenGlHelper.glGetProgrami(this.glId, OpenGlHelper.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + OpenGlHelperExt.getLogInfo(this.glId));
    
        }
        catch(Exception e)
        {
            if(this.glId > 0)
                OpenGlHelper.glDeleteShader(glId);
            
            RenderHooks.INSTANCE.getLog().error("Unable to create shader " + this.fileName, e);
            this.glId = -1;
        }
    }
}
