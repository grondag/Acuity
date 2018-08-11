package grondag.acuity.api;

import org.lwjgl.util.vector.Matrix4f;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IUniform
{
    public interface IUniform1f extends IUniform
    {
        void set(float v0);
    }
    
    public interface IUniform2f extends IUniform
    {
        void set(float v0, float v1);
    }
    
    public interface IUniform3f extends IUniform
    {
        void set(float v0, float v1, float v2);
    }
    
    public interface IUniform4f extends IUniform
    {
        void set(float v0, float v1, float v2, float v3);
    }
    
    public interface IUniform1i extends IUniform
    {
        void set(int v0);
    }
    
    public interface IUniform2i extends IUniform
    {
        void set(int v0, int v1);
    }
    
    public interface IUniform3i extends IUniform
    {
        void set(int v0, int v1, int v2);
    }
    
    public interface IUniform4i extends IUniform
    {
        void set(int v0, int v1, int v2, int v3);
    }
    
    public interface IUniformMatrix4f extends IUniform
    {
        void set(float... elements);

        void set(Matrix4f matrix);
    }
}
