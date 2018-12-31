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

package grondag.acuity.api;

import java.util.function.Consumer;

import grondag.acuity.Configurator;
import grondag.acuity.api.PipelineUniform.Uniform1f;
import grondag.acuity.api.PipelineUniform.Uniform1i;
import grondag.acuity.api.PipelineUniform.Uniform2f;
import grondag.acuity.api.PipelineUniform.Uniform2i;
import grondag.acuity.api.PipelineUniform.Uniform3f;
import grondag.acuity.api.PipelineUniform.Uniform3i;
import grondag.acuity.api.PipelineUniform.Uniform4f;
import grondag.acuity.api.PipelineUniform.Uniform4i;
import grondag.acuity.api.PipelineUniform.UniformMatrix4f;
import grondag.acuity.fermion.config.Localization;
import grondag.acuity.pipeline.PipelineFragmentShader;
import grondag.acuity.pipeline.PipelineShaderManager;
import grondag.acuity.pipeline.PipelineVertexShader;
import grondag.acuity.pipeline.Program;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormat;

@Environment(EnvType.CLIENT)
public final class RenderPipelineImpl implements RenderPipeline
{
    private final int index;
    private final Program solidProgram;
    private final Program translucentProgram;
    public final TextureDepth textureFormat;
    private boolean isFinal = false;
    private PipelineVertexFormat pipelineVertexFormat;
    private VertexFormat vertexFormat;
    
    RenderPipelineImpl(int index, String vertexShader, String fragmentShader, TextureDepth textureFormat)
    {
        PipelineVertexShader  vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, textureFormat, true);
        PipelineFragmentShader  fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, textureFormat, true);
        this.solidProgram = new Program(vs, fs, textureFormat, true);
        
        vs = PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, textureFormat, false);
        fs = PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, textureFormat, false);
        this.translucentProgram = new Program(vs, fs, textureFormat, false);
        
        this.index = index;
        this.textureFormat = textureFormat;
        this.refreshVertexFormats();
    }
    
    public void activate(boolean isSolidLayer)
    {
        if(isSolidLayer)
            this.solidProgram.activate();
        else
            this.translucentProgram.activate();
    }
    
    public void forceReload()
    {
        this.solidProgram.forceReload();
        this.translucentProgram.forceReload();
        this.refreshVertexFormats();
    }
    
    public void refreshVertexFormats()
    {
        this.pipelineVertexFormat = Configurator.lightingModel.vertexFormat(this.textureFormat);
        this.vertexFormat = this.pipelineVertexFormat.vertexFormat;
    }
    
    @Override
    public TextureDepth textureDepth()
    {
        return this.textureFormat;
    }
    
    @Override
    public RenderPipeline finish()
    {
        this.isFinal = true;
        this.forceReload();
        return this;
    }
    
    public PipelineVertexFormat piplineVertexFormat()
    {
        return this.pipelineVertexFormat;
    }
    
    /**
     * Avoids a pointer chase, more concise code.
     */
    public VertexFormat vertexFormat()
    {
        return this.vertexFormat;
    }
    
    @Override
    public int getIndex()
    {
        return this.index;
    }

    private void checkFinal()
    {
        if(this.isFinal)
            throw new UnsupportedOperationException(Localization.translate("misc.warn_uniform_program_immutable_exception"));    
    }
    
    public void uniformSampler2d(String name, UniformUpdateFrequency frequency, Consumer<Uniform1i> initializer)
    {
        if(solidProgram.containsUniformSpec("sampler2D", name))
            solidProgram.uniform1i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("sampler2D", name))
            translucentProgram.uniform1i(name, frequency, initializer);
    }
    
    @Override
    public void uniform1f(String name, UniformUpdateFrequency frequency, Consumer<Uniform1f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("float", name))
            solidProgram.uniform1f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("float", name))
            translucentProgram.uniform1f(name, frequency, initializer);
    }

    @Override
    public void uniform2f(String name, UniformUpdateFrequency frequency, Consumer<Uniform2f> initializer)
    {
        checkFinal();    
        if(solidProgram.containsUniformSpec("vec2", name))
            solidProgram.uniform2f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("vec2", name))
            translucentProgram.uniform2f(name, frequency, initializer);
    }

    @Override
    public void uniform3f(String name, UniformUpdateFrequency frequency, Consumer<Uniform3f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("vec3", name))
            solidProgram.uniform3f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("vec3", name))
            translucentProgram.uniform3f(name, frequency, initializer);        
    }

    @Override
    public void uniform4f(String name, UniformUpdateFrequency frequency, Consumer<Uniform4f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("vec4", name))
            solidProgram.uniform4f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("vec4", name))
            translucentProgram.uniform4f(name, frequency, initializer);        
    }

    @Override
    public void uniform1i(String name, UniformUpdateFrequency frequency, Consumer<Uniform1i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("int", name))
            solidProgram.uniform1i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("int", name))
            translucentProgram.uniform1i(name, frequency, initializer);        
    }

    @Override
    public void uniform2i(String name, UniformUpdateFrequency frequency, Consumer<Uniform2i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("ivec2", name))
            solidProgram.uniform2i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("ivec2", name))
            translucentProgram.uniform2i(name, frequency, initializer);         
    }

    @Override
    public void uniform3i(String name, UniformUpdateFrequency frequency, Consumer<Uniform3i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("ivec3", name))
            solidProgram.uniform3i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("ivec3", name))
            translucentProgram.uniform3i(name, frequency, initializer);           
    }

    @Override
    public void uniform4i(String name, UniformUpdateFrequency frequency, Consumer<Uniform4i> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("ivec4", name))
            solidProgram.uniform4i(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("ivec4", name))
            translucentProgram.uniform4i(name, frequency, initializer);         
    }

    @Override
    public void uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<UniformMatrix4f> initializer)
    {
        checkFinal();
        if(solidProgram.containsUniformSpec("mat4", name))
            solidProgram.uniformMatrix4f(name, frequency, initializer);
        if(translucentProgram.containsUniformSpec("mat4", name))
            translucentProgram.uniformMatrix4f(name, frequency, initializer);         
    }

    public void onRenderTick()
    {
        this.solidProgram.onRenderTick();
        this.translucentProgram.onRenderTick();
    }

    public void onGameTick()
    {
        this.solidProgram.onGameTick();
        this.translucentProgram.onGameTick();        
    }

    public void setupModelViewUniforms()
    {
        this.solidProgram.setupModelViewUniforms();
        this.translucentProgram.setupModelViewUniforms();
    }
}
