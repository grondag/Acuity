package grondag.acuity.core;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Acuity;
import grondag.acuity.Configurator;
import grondag.acuity.api.IUniform;
import grondag.acuity.api.IUniform.IUniform1f;
import grondag.acuity.api.IUniform.IUniform1i;
import grondag.acuity.api.IUniform.IUniform2f;
import grondag.acuity.api.IUniform.IUniform2i;
import grondag.acuity.api.IUniform.IUniform3f;
import grondag.acuity.api.IUniform.IUniform3i;
import grondag.acuity.api.IUniform.IUniform4f;
import grondag.acuity.api.IUniform.IUniform4i;
import grondag.acuity.api.IUniform.IUniformMatrix4f;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.api.UniformUpdateFrequency;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Program
{
    private int progID = -1;
    private boolean needsLoad = true;
    private boolean isErrored = false;

    public final PipelineVertexShader vertexShader;
    public final PipelineFragmentShader fragmentShader;
    public final TextureFormat textureFormat;
    public final boolean isSolidLayer;
 
    private final ObjectArrayList<Uniform<?>> uniforms = new ObjectArrayList<>();
    protected final ObjectArrayList<Uniform<?>> dirtyUniforms = new ObjectArrayList<>();
    private final ObjectArrayList<Uniform<?>> renderTickUpdates = new ObjectArrayList<>();
    private final ObjectArrayList<Uniform<?>> gameTickUpdates = new ObjectArrayList<>();
    
    public abstract class Uniform<T extends IUniform>
    {
        private final String name;
        protected boolean isUniformDirty = true;
        protected boolean needsInitialization = true;
        protected int unifID = -1;
        protected final @Nullable Consumer<T> initializer;
        protected final@Nullable UniformUpdateFrequency frequency;
        
        protected Uniform(String name, @Nullable Consumer<T> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            this.name = name;
            this.initializer = initializer;
            this.frequency = frequency;
            this.needsInitialization = initializer != null;
            this.setDirty();
        }
        
        protected void setDirty()
        {
            if(!this.isUniformDirty)
            {
                this.isUniformDirty = true;
                dirtyUniforms.add(this);
            }
        }
        
        protected void markForInitialization()
        {
            if(this.initializer != null)
            {
                this.needsInitialization = true;
                dirtyUniforms.add(this);
            }
        }
        
        @SuppressWarnings({ "unchecked"})
        protected void initialize()
        {
            if(this.needsInitialization && this.initializer != null)
            {
                this.initializer.accept((T) this);
                this.needsInitialization = false;
            }
        }
        
        private void load(int programID)
        {
            this.unifID = OpenGlHelper.glGetUniformLocation(programID, name);
            if(this.unifID == -1)
            {
                Acuity.INSTANCE.getLog().debug(I18n.translateToLocalFormatted("misc.debug_missing_uniform", name, Program.this.vertexShader.fileName, Program.this.fragmentShader.fileName));
                this.isUniformDirty = false;
            }
            else
            {
                this.markForInitialization();
                this.isUniformDirty = true;
            }
        }
        
        protected final void upload()
        {
            if(this.unifID >= 0)
            {
                this.initialize();
                if(this.isUniformDirty)
                {
                    this.isUniformDirty = false;
                    this.uploadInner();
                }
            }
        }
        
        protected abstract void uploadInner();
    }
    
    protected abstract class UniformFloat<T extends IUniform> extends Uniform<T>
    {
        protected final FloatBuffer uniformFloatBuffer;
        
        protected UniformFloat(String name, @Nullable Consumer<T> initializer, @Nullable UniformUpdateFrequency frequency, int size)
        {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = BufferUtils.createFloatBuffer(size);
        }
    }
    
    public class Uniform1f extends UniformFloat<IUniform1f> implements IUniform1f
    {
        protected Uniform1f(String name, @Nullable Consumer<IUniform1f> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 1);
        }

        @Override
        public void set(float value)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != value)
            {
                this.uniformFloatBuffer.put(0, value);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform1(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    public class Uniform2f extends UniformFloat<IUniform2f> implements IUniform2f
    {
        protected Uniform2f(String name, @Nullable Consumer<IUniform2f> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 2);
        }

        @Override
        public void set(float v0, float v1)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != v0)
            {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(1) != v1)
            {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform2(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    public class Uniform3f extends UniformFloat<IUniform3f> implements IUniform3f
    {
        protected Uniform3f(String name, @Nullable Consumer<IUniform3f> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 3);
        }

        @Override
        public void set(float v0, float v1, float v2)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != v0)
            {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(1) != v1)
            {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(2) != v2)
            {
                this.uniformFloatBuffer.put(2, v2);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform3(this.unifID, this.uniformFloatBuffer);
       }
    }
    
    public class Uniform4f extends UniformFloat<IUniform4f> implements IUniform4f
    {
        protected Uniform4f(String name, @Nullable Consumer<IUniform4f> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 4);
        }

        @Override
        public void set(float v0, float v1, float v2, float v3)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != v0)
            {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(1) != v1)
            {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(2) != v2)
            {
                this.uniformFloatBuffer.put(2, v2);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(3) != v3)
            {
                this.uniformFloatBuffer.put(3, v3);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform4(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    private <T extends Uniform<?>> T addUniform(T toAdd)
    {
        this.uniforms.add(toAdd);
        if(toAdd.frequency == UniformUpdateFrequency.PER_FRAME)
            this.renderTickUpdates.add(toAdd);
        else if(toAdd.frequency == UniformUpdateFrequency.PER_TICK)
            this.gameTickUpdates.add(toAdd);
        return toAdd;
    }
    
    public IUniform1f uniform1f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform1f> initializer)
    {
        return addUniform(new Uniform1f(name, initializer, frequency));
    }
    
    public IUniform2f uniform2f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform2f> initializer)
    {
        return addUniform(new Uniform2f(name, initializer, frequency));
    }
    
    public IUniform3f uniform3f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform3f> initializer)
    {
        return addUniform(new Uniform3f(name, initializer, frequency));
    }
    
    public IUniform4f uniform4f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform4f> initializer)
    {
        return addUniform(new Uniform4f(name, initializer, frequency));
    }
    
    protected abstract class UniformInt<T extends IUniform> extends Uniform<T>
    {
        protected final IntBuffer uniformIntBuffer;
        
        protected UniformInt(String name, @Nullable Consumer<T> initializer, @Nullable UniformUpdateFrequency frequency, int size)
        {
            super(name, initializer, frequency);
            this.uniformIntBuffer = BufferUtils.createIntBuffer(size);
        }
    }
    
    public class Uniform1i extends UniformInt<IUniform1i> implements IUniform1i
    {
        protected Uniform1i(String name, @Nullable Consumer<IUniform1i> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 1);
        }

        @Override
        public void set(int value)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != value)
            {
                this.uniformIntBuffer.put(0, value);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform1(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform2i extends UniformInt<IUniform2i> implements IUniform2i
    {
        protected Uniform2i(String name, @Nullable Consumer<IUniform2i> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 2);
        }

        @Override
        public void set(int v0, int v1)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != v0)
            {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(1) != v1)
            {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform2(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform3i extends UniformInt<IUniform3i> implements IUniform3i
    {
        protected Uniform3i(String name, @Nullable Consumer<IUniform3i> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 3);
        }

        @Override
        public void set(int v0, int v1, int v2)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != v0)
            {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(1) != v1)
            {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(2) != v2)
            {
                this.uniformIntBuffer.put(2, v2);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform3(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform4i extends UniformInt<IUniform4i> implements IUniform4i
    {
        protected Uniform4i(String name, @Nullable Consumer<IUniform4i> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 4);
        }

        @Override
        public void set(int v0, int v1, int v2, int v3)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != v0)
            {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(1) != v1)
            {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(2) != v2)
            {
                this.uniformIntBuffer.put(2, v2);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(3) != v3)
            {
                this.uniformIntBuffer.put(3, v3);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform4(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public IUniform1i uniform1i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform1i> initializer)
    {
        return addUniform(new Uniform1i(name, initializer, frequency));
    }
    
    public IUniform2i uniform2i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform2i> initializer)
    {
        return addUniform(new Uniform2i(name, initializer, frequency));
    }
    
    public IUniform3i uniform3i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform3i> initializer)
    {
        return addUniform(new Uniform3i(name, initializer, frequency));
    }
    
    public IUniform4i uniform4i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform4i> initializer)
    {
        return addUniform(new Uniform4i(name, initializer, frequency));
    }
    
    public Program(PipelineVertexShader vertexShader, PipelineFragmentShader fragmentShader, TextureFormat textureFormat, boolean isSolidLayer)
    {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.textureFormat = textureFormat;
        this.isSolidLayer = isSolidLayer;
    }
 
    /**
     * Call after render / resource refresh to force shader reload.
     */
    public void forceReload()
    {
        this.needsLoad = true;
    }

    
    public void activate()
    {
        if(this.needsLoad)
            this.load();
        
        final int progID = this.progID;
        if(progID <= 0 || this.isErrored)
            return;
        
        OpenGlHelperExt.glUseProgramFast(progID);
        
        if(!this.dirtyUniforms.isEmpty())
        {
            this.dirtyUniforms.forEach(u -> u.upload());
            this.dirtyUniforms.clear();
        }
    }
    
    public class UniformMatrix4f extends Uniform<IUniformMatrix4f> implements IUniformMatrix4f
    {
        protected final FloatBuffer uniformFloatBuffer;
        
        protected UniformMatrix4f(String name, @Nullable Consumer<IUniformMatrix4f> initializer, @Nullable UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = BufferUtils.createFloatBuffer(16);
        }

        @Override
        public void set(Matrix4f matrix)
        {
            this.set(matrix.m00, matrix.m01, matrix.m02, matrix.m03, matrix.m10, matrix.m11, matrix.m12, matrix.m13, matrix.m20, matrix.m21, matrix.m22, matrix.m23, matrix.m30, matrix.m31, matrix.m32, matrix.m33);
        }
        
        @Override
        public void set(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33)
        {
            if(this.unifID == -1) return;
            if(!(   this.uniformFloatBuffer.get(0) == m00
                 && this.uniformFloatBuffer.get(1) == m01
                 && this.uniformFloatBuffer.get(2) == m02
                 && this.uniformFloatBuffer.get(3) == m03
                 && this.uniformFloatBuffer.get(4) == m10
                 && this.uniformFloatBuffer.get(5) == m11
                 && this.uniformFloatBuffer.get(6) == m12
                 && this.uniformFloatBuffer.get(7) == m13
                 && this.uniformFloatBuffer.get(8) == m20
                 && this.uniformFloatBuffer.get(9) == m21
                 && this.uniformFloatBuffer.get(10) == m22
                 && this.uniformFloatBuffer.get(11) == m23
                 && this.uniformFloatBuffer.get(12) == m30
                 && this.uniformFloatBuffer.get(13) == m31
                 && this.uniformFloatBuffer.get(14) == m32
                 && this.uniformFloatBuffer.get(15) == m33))
            {
                this.uniformFloatBuffer.put(0, m00);
                this.uniformFloatBuffer.put(1, m01);
                this.uniformFloatBuffer.put(2, m02);
                this.uniformFloatBuffer.put(3, m03);
                this.uniformFloatBuffer.put(4, m10);
                this.uniformFloatBuffer.put(5, m11);
                this.uniformFloatBuffer.put(6, m12);
                this.uniformFloatBuffer.put(7, m13);
                this.uniformFloatBuffer.put(8, m20);
                this.uniformFloatBuffer.put(9, m21);
                this.uniformFloatBuffer.put(10, m22);
                this.uniformFloatBuffer.put(11, m23);
                this.uniformFloatBuffer.put(12, m30);
                this.uniformFloatBuffer.put(13, m31);
                this.uniformFloatBuffer.put(14, m32);
                this.uniformFloatBuffer.put(15, m33);
                this.uniformFloatBuffer.position(0);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniformMatrix4(this.unifID, true, this.uniformFloatBuffer);
        }
    }
    
    public UniformMatrix4f uniformMatrix4f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniformMatrix4f> initializer)
    {
        return addUniform(new UniformMatrix4f(name, initializer, frequency));
    }
    
    /**
     * NB: Not necessary to call if going to activate a different shader.
     */
    public void deactivate()
    {
        OpenGlHelper.glUseProgram(0);
    }
    
    private void load()
    {
        this.needsLoad = false;
        this.isErrored = true;
        try
        {
            if(this.progID > 0)
                OpenGlHelper.glDeleteProgram(progID);
            
            this.progID = OpenGlHelper.glCreateProgram();
            
            this.isErrored = this.progID > 0 && !loadInner();
        }
        catch(Exception e)
        {
            if(this.progID > 0)
                OpenGlHelper.glDeleteProgram(progID);
            
            Acuity.INSTANCE.getLog().error(I18n.translateToLocal("misc.error_program_link_failure"), e);
            this.progID = -1;
        }
        
        if(!this.isErrored)
        {
            this.uniforms.forEach(u -> u.load(progID));
        }
        
    }
    
    /**
     * Return true on success
     */
    private boolean loadInner()
    {
        final int programID = this.progID;
        if(programID <= 0)
            return false;
        
        final int vertId = vertexShader.glId();
        if(vertId <= 0)
            return false;
        
        final int fragId = fragmentShader.glId();
        if(fragId <= 0)
            return false;
        
        OpenGlHelper.glAttachShader(programID, vertId);
        OpenGlHelper.glAttachShader(programID, fragId);
        
        Configurator.lightingModel.vertexFormat(this.textureFormat).bindProgramAttributes(programID);
        
        OpenGlHelper.glLinkProgram(programID);
        if(OpenGlHelper.glGetProgrami(programID, OpenGlHelper.GL_LINK_STATUS) == GL11.GL_FALSE)
        {
            Acuity.INSTANCE.getLog().error(OpenGlHelperExt.getProgramInfoLog(programID));
            return false;
        }

        
        return true;
    }

    public void onRenderTick()
    {
        this.renderTickUpdates.forEach(u -> u.markForInitialization());
    }

    public void onGameTick()
    {
        this.gameTickUpdates.forEach(u -> u.markForInitialization());
    }
    
    // FAIL: unfortunately using explicit uniforms is slower
//    protected @Nullable UniformMatrix4f modelViewUniform;
//    protected @Nullable UniformMatrix4f modelViewProjectionUniform;
//    public void setupModelViewUniforms()
//    {
//        if(containsUniformSpec(this, "mat4", "u_modelView"))
//        {
//            this.modelViewUniform = this.uniformMatrix4f("u_modelView", UniformUpdateFrequency.ON_LOAD, u -> 
//            {
//                // NOOP - will be set as needed
//            });
//        }
//
//        if(containsUniformSpec(this, "mat4", "u_modelViewProjection"))
//        {
//            this.modelViewProjectionUniform = this.uniformMatrix4f("u_modelViewProjection", UniformUpdateFrequency.ON_LOAD, u -> 
//            {
//                // NOOP - will be set as needed
//            });
//        }
//    }
    
    public boolean containsUniformSpec(String type, String name)
    {
        String regex = "(?m)^uniform\\s+" + type + "\\s+" + name + "\\s*;";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(this.vertexShader.getSource()).find() 
                || pattern.matcher(this.fragmentShader.getSource()).find(); 
    }
}