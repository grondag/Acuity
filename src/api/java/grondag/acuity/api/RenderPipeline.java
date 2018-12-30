package grondag.acuity.api;

import java.util.function.Consumer;

import grondag.acuity.api.PipelineUniform.Uniform1f;
import grondag.acuity.api.PipelineUniform.Uniform1i;
import grondag.acuity.api.PipelineUniform.Uniform2f;
import grondag.acuity.api.PipelineUniform.Uniform2i;
import grondag.acuity.api.PipelineUniform.Uniform3f;
import grondag.acuity.api.PipelineUniform.Uniform3i;
import grondag.acuity.api.PipelineUniform.Uniform4f;
import grondag.acuity.api.PipelineUniform.Uniform4i;
import grondag.acuity.api.PipelineUniform.UniformMatrix4f;
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

    /**
     * Call after all uniforms are added to make this program immutable.  Any attempt to add uniforms after calling
     * {@link #finish()} will throw an exception.  Not strictly necessary, but good practice.<p>
     * 
     * Note that all built-in pipelines are finished - you cannot add uniforms to them.
     */
    RenderPipeline finish();

    void uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<UniformMatrix4f> initializer);
}
