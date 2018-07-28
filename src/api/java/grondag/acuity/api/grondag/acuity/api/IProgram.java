package grondag.acuity.api;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.api.Program.Uniform1f;
import grondag.acuity.api.Program.Uniform1i;
import grondag.acuity.api.Program.Uniform2f;
import grondag.acuity.api.Program.Uniform2i;
import grondag.acuity.api.Program.Uniform3f;
import grondag.acuity.api.Program.Uniform3i;
import grondag.acuity.api.Program.Uniform4f;
import grondag.acuity.api.Program.Uniform4i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IProgram
{
    Uniform1f uniform1f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform1f> initializer);

    Uniform2f uniform2f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform2f> initializer);

    Uniform3f uniform3f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform3f> initializer);

    Uniform4f uniform4f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform4f> initializer);

    Uniform1i uniform1i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform1i> initializer);

    Uniform2i uniform2i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform2i> initializer);

    Uniform3i uniform3i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform3i> initializer);

    Uniform4i uniform4i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<Uniform4i> initializer);

    /**
     * Call after all uniforms are added to make this program immutable.  Any attempt to add uniforms after calling
     * {@link #finish()} will throw and exception.  Not strictly necessary, but good practice.<p>
     * 
     * Note that all built-in pipelines are finished - you cannot add uniforms to them.
     */
    IProgram finish();

}