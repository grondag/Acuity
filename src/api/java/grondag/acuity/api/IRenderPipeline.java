package grondag.acuity.api;

import java.util.function.Consumer;

import grondag.acuity.api.IUniform.IUniform1f;
import grondag.acuity.api.IUniform.IUniform1i;
import grondag.acuity.api.IUniform.IUniform2f;
import grondag.acuity.api.IUniform.IUniform2i;
import grondag.acuity.api.IUniform.IUniform3f;
import grondag.acuity.api.IUniform.IUniform3i;
import grondag.acuity.api.IUniform.IUniform4f;
import grondag.acuity.api.IUniform.IUniform4i;
import grondag.acuity.api.IUniform.IUniformMatrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Type-safe reference to a rendering pipeline.
 */
@Environment(EnvType.CLIENT)
public interface IRenderPipeline
{
    int getIndex();
    
    TextureFormat textureFormat();
    
    void uniform1f(String name, UniformUpdateFrequency frequency, Consumer<IUniform1f> initializer);

    void uniform2f(String name, UniformUpdateFrequency frequency, Consumer<IUniform2f> initializer);

    void uniform3f(String name, UniformUpdateFrequency frequency, Consumer<IUniform3f> initializer);

    void uniform4f(String name, UniformUpdateFrequency frequency, Consumer<IUniform4f> initializer);

    void uniform1i(String name, UniformUpdateFrequency frequency, Consumer<IUniform1i> initializer);

    void uniform2i(String name, UniformUpdateFrequency frequency, Consumer<IUniform2i> initializer);

    void uniform3i(String name, UniformUpdateFrequency frequency, Consumer<IUniform3i> initializer);

    void uniform4i(String name, UniformUpdateFrequency frequency, Consumer<IUniform4i> initializer);

    /**
     * Call after all uniforms are added to make this program immutable.  Any attempt to add uniforms after calling
     * {@link #finish()} will throw an exception.  Not strictly necessary, but good practice.<p>
     * 
     * Note that all built-in pipelines are finished - you cannot add uniforms to them.
     */
    IRenderPipeline finish();

    void uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<IUniformMatrix4f> initializer);
}
