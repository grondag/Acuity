package grondag.acuity.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.io.CharStreams;

import grondag.acuity.Acuity;
import grondag.acuity.core.OpenGlHelperExt;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
abstract class AbstractPipelineShader
{
    public final String fileName;
    
    private final int shaderType;
    public final TextureFormat textureFormat;

    private int glId = -1;
    private boolean needsLoad = true;
    private boolean isErrored = false;
    
    AbstractPipelineShader(String fileName, int shaderType, TextureFormat textureFormat)
    {
        this.fileName = fileName;
        this.shaderType = shaderType;
        this.textureFormat = textureFormat;
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
        
        return this.isErrored ? -1 : this.glId;
    }
    
    private final void load()
    {
        this.needsLoad = false;
        this.isErrored = false;
        try
        {
            String source = this.getSource();
            
            byte[] abyte = source.getBytes();
            ByteBuffer bytebuffer = BufferUtils.createByteBuffer(abyte.length);
            bytebuffer.put(abyte);
            bytebuffer.position(0);
            
            if(this.glId <= 0)
            {
                this.glId = OpenGlHelper.glCreateShader(shaderType);
                if(this.glId == 0) 
                {
                    this.glId = -1;
                    this.isErrored = true;
                    return;
                }
            }
            
            OpenGlHelper.glShaderSource(this.glId, bytebuffer);
            OpenGlHelper.glCompileShader(this.glId);
    
            if (OpenGlHelper.glGetShaderi(this.glId, OpenGlHelper.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException(OpenGlHelperExt.getShaderInfoLog(this.glId));
    
        }
        catch(Exception e)
        {
            this.isErrored = true;
            if(this.glId > 0)
            {
                OpenGlHelper.glDeleteShader(glId);
                this.glId = -1;
            }
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.fail_create_shader", this.fileName, e.getMessage()));
        }
    }
    
    public String getSource()
    {
        return getShaderSource(this.fileName);
    }
    
    public static String getShaderSource(String fileName)
    {
        InputStream in = ProgramManager.class.getResourceAsStream(fileName);
        
        if(in == null)
            return "";
        
        try (final Reader reader = new InputStreamReader(in))
        {
            return CharStreams.toString(reader);
        }
        catch (IOException e)
        {
            return "";
        }
    }
}
