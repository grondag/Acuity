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

package grondag.acuity.api.pipeline;

import java.util.function.Consumer;

import grondag.acuity.api.model.TextureDepth;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform1f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform1i;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform2f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform2i;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform3f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform3i;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform4f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform4i;
import grondag.acuity.api.pipeline.PipelineUniform.UniformMatrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Type-safe reference to a rendering pipeline.
 */
@Environment(EnvType.CLIENT)
public interface RenderPipeline
{
    int getIndex();
    
    TextureDepth textureDepth();
    
    void uniform1f(String name, UniformUpdateFrequency frequency, Consumer<Uniform1f> initializer);

    void uniform2f(String name, UniformUpdateFrequency frequency, Consumer<Uniform2f> initializer);

    void uniform3f(String name, UniformUpdateFrequency frequency, Consumer<Uniform3f> initializer);

    void uniform4f(String name, UniformUpdateFrequency frequency, Consumer<Uniform4f> initializer);

    void uniform1i(String name, UniformUpdateFrequency frequency, Consumer<Uniform1i> initializer);

    void uniform2i(String name, UniformUpdateFrequency frequency, Consumer<Uniform2i> initializer);

    void uniform3i(String name, UniformUpdateFrequency frequency, Consumer<Uniform3i> initializer);

    void uniform4i(String name, UniformUpdateFrequency frequency, Consumer<Uniform4i> initializer);

    void uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<UniformMatrix4f> initializer);

    /**
     * Call after all uniforms are added to make this program immutable.  Any attempt to add uniforms after calling
     * {@link #finish()} will throw an exception.  Not strictly necessary, but good practice.<p>
     * 
     * Note that all built-in pipelines are finished - you cannot add uniforms to them.
     */
    RenderPipeline finish();
}
