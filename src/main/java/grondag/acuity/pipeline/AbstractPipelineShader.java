/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.lwjgl.opengl.GL11;

import com.google.common.io.CharStreams;
import com.mojang.blaze3d.platform.GLX;

import grondag.acuity.Acuity;
import grondag.acuity.api.PipelineManagerImpl;
import grondag.acuity.api.TextureDepth;
import grondag.acuity.fermion.config.Localization;
import grondag.acuity.opengl.OpenGlHelperExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
abstract class AbstractPipelineShader
{
    public final String fileName;
    
    private final int shaderType;
    public final TextureDepth textureFormat;
    public final boolean isSolidLayer;

    private int glId = -1;
    private boolean needsLoad = true;
    private boolean isErrored = false;
    
    AbstractPipelineShader(String fileName, int shaderType, TextureDepth textureFormat, boolean isSolidLayer)
    {
        this.fileName = fileName;
        this.shaderType = shaderType;
        this.textureFormat = textureFormat;
        this.isSolidLayer = isSolidLayer;
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
            
            if(this.glId <= 0)
            {
                this.glId = GLX.glCreateShader(shaderType);
                if(this.glId == 0) 
                {
                    this.glId = -1;
                    this.isErrored = true;
                    return;
                }
            }
            
            GLX.glShaderSource(this.glId, source);
            GLX.glCompileShader(this.glId);
    
            if (GLX.glGetShaderi(this.glId, GLX.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException(OpenGlHelperExt.getShaderInfoLog(this.glId));
    
        }
        catch(Exception e)
        {
            this.isErrored = true;
            if(this.glId > 0)
            {
                GLX.glDeleteShader(glId);
                this.glId = -1;
            }
            Acuity.INSTANCE.getLog().error(Localization.translate("misc.fail_create_shader", this.fileName, this.textureFormat.toString(), e.getMessage()));
        }
    }
    
    public String buildSource(String librarySource)
    {
        String result = getShaderSource(this.fileName);
        result = result.replaceAll("#version\\s+120", "");
        result = librarySource + result;
        
        final int layerCount = this.textureFormat.layerCount();
        if(layerCount > 1)
            result = result.replaceAll("#define LAYER_COUNT 1", String.format("#define LAYER_COUNT %d", layerCount));
        
        if(!isSolidLayer)
            result = result.replaceAll("#define SOLID", "#define TRANSLUCENT");
        
        return result;
    }
    
    abstract String getSource();
    
    public static String getShaderSource(String fileName)
    {
        InputStream in = PipelineManagerImpl.class.getResourceAsStream(fileName);
        
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
