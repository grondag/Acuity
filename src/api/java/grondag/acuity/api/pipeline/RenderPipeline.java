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
 * Type-safe reference to a rendering pipeline. <p>
 * 
 * You need this to create uniforms (for pipelines you create)
 * and to specify pipeline for quads that you are sending to VertexConsumer.<p>
 * 
 * Currently there is no pipeline registry and thus no built-in way
 * for mods to share pipelines.  If this is needed, open an issue or PR.
 */
@Environment(EnvType.CLIENT)
public interface RenderPipeline
{
    /**
     * Unique identifier. Transient - may change in subsequent game sessions.<p>
     * 
     * Use this if you need to serialize a pipeline reference into a primitive value.
     * You can then use {@link PipelineManager#getPipelineByIndex(int)} to retrieve it.
     * Again, remember the ID is transient and thus not suitable for world saves.
     */
    int getIndex();
    
    /**
     * Defines how many texture layers are expected by this pipeline. Implies {@link PipelineVertexFormat}
     */
    TextureDepth textureDepth();
    
    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform1f(String name, UniformUpdateFrequency frequency, Consumer<Uniform1f> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform2f(String name, UniformUpdateFrequency frequency, Consumer<Uniform2f> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform3f(String name, UniformUpdateFrequency frequency, Consumer<Uniform3f> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform4f(String name, UniformUpdateFrequency frequency, Consumer<Uniform4f> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform1i(String name, UniformUpdateFrequency frequency, Consumer<Uniform1i> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform2i(String name, UniformUpdateFrequency frequency, Consumer<Uniform2i> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform3i(String name, UniformUpdateFrequency frequency, Consumer<Uniform3i> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniform4i(String name, UniformUpdateFrequency frequency, Consumer<Uniform4i> initializer);

    /**
     * Creates a new uniform for this pipeline. See {@link UniformUpdateFrequency} for additional info.
     */
    void uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<UniformMatrix4f> initializer);

    /**
     * Call after all uniforms are added to make this program immutable.  Any attempt to add uniforms after calling
     * {@link #finish()} will throw an exception.  Not strictly necessary, but good practice.<p>
     * 
     * Note that all built-in pipelines are finished - you cannot add uniforms to them.
     */
    RenderPipeline finish();
}
