package grondag.render_hooks.api;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.render_hooks.api.Program.Uniform1f;
import grondag.render_hooks.api.Program.Uniform1i;
import grondag.render_hooks.api.Program.Uniform2f;
import grondag.render_hooks.api.Program.Uniform2i;
import grondag.render_hooks.api.Program.Uniform3f;
import grondag.render_hooks.api.Program.Uniform3i;
import grondag.render_hooks.api.Program.Uniform4f;
import grondag.render_hooks.api.Program.Uniform4i;

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

    IProgram finish();

}