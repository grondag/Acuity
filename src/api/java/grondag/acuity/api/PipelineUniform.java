package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.Matrix4f;

@Environment(EnvType.CLIENT)
public interface PipelineUniform
{
    public interface Uniform1f extends PipelineUniform
    {
        void set(float v0);
    }
    
    public interface Uniform2f extends PipelineUniform
    {
        void set(float v0, float v1);
    }
    
    public interface Uniform3f extends PipelineUniform
    {
        void set(float v0, float v1, float v2);
    }
    
    public interface Uniform4f extends PipelineUniform
    {
        void set(float v0, float v1, float v2, float v3);
    }
    
    public interface Uniform1i extends PipelineUniform
    {
        void set(int v0);
    }
    
    public interface Uniform2i extends PipelineUniform
    {
        void set(int v0, int v1);
    }
    
    public interface Uniform3i extends PipelineUniform
    {
        void set(int v0, int v1, int v2);
    }
    
    public interface Uniform4i extends PipelineUniform
    {
        void set(int v0, int v1, int v2, int v3);
    }
    
    public interface UniformMatrix4f extends PipelineUniform
    {
        void set(float... elements);

        void set(Matrix4f matrix);
    }
}
