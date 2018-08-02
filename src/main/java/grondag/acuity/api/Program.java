package grondag.acuity.api;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import grondag.acuity.Configurator;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.api.UniformUpdateFrequency;
import grondag.acuity.api.IUniform.IUniform1f;
import grondag.acuity.api.IUniform.IUniform1i;
import grondag.acuity.api.IUniform.IUniform2f;
import grondag.acuity.api.IUniform.IUniform2i;
import grondag.acuity.api.IUniform.IUniform3f;
import grondag.acuity.api.IUniform.IUniform3i;
import grondag.acuity.api.IUniform.IUniform4f;
import grondag.acuity.api.IUniform.IUniform4i;
import grondag.acuity.Acuity;
import grondag.acuity.core.OpenGlHelperExt;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class Program implements IRenderPipeline
{
    private int progID = -1;
    private boolean needsLoad = true;
    private boolean isErrored = false;
    private boolean isFinal = false;
    protected boolean hasDirtyUniform = false;
    
    public final PipelineVertexShader vertexShader;
    public final PipelineFragmentShader fragmentShader;
    public final TextureFormat textureFormat;
    
    private final ObjectArrayList<Uniform<?>> uniforms = new ObjectArrayList<>();
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
            this.isUniformDirty = true;
            hasDirtyUniform = true;
        }
        
        protected void markForInitialization()
        {
            if(this.initializer != null)
            {
                this.needsInitialization = true;
                hasDirtyUniform = true;
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
        if(this.isFinal)
            throw new UnsupportedOperationException(I18n.translateToLocal("misc.warn_uniform_program_immutable_exception"));
        
        this.uniforms.add(toAdd);
        if(toAdd.frequency == UniformUpdateFrequency.PER_FRAME)
            this.renderTickUpdates.add(toAdd);
        else if(toAdd.frequency == UniformUpdateFrequency.PER_TICK)
            this.gameTickUpdates.add(toAdd);
        return toAdd;
    }
    
    @Override
    public IUniform1f uniform1f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform1f> initializer)
    {
        return addUniform(new Uniform1f(name, initializer, frequency));
    }
    
    @Override
    public IUniform2f uniform2f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform2f> initializer)
    {
        return addUniform(new Uniform2f(name, initializer, frequency));
    }
    
    @Override
    public IUniform3f uniform3f(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform3f> initializer)
    {
        return addUniform(new Uniform3f(name, initializer, frequency));
    }
    
    @Override
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
    
    @Override
    public IUniform1i uniform1i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform1i> initializer)
    {
        return addUniform(new Uniform1i(name, initializer, frequency));
    }
    
    @Override
    public IUniform2i uniform2i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform2i> initializer)
    {
        return addUniform(new Uniform2i(name, initializer, frequency));
    }
    
    @Override
    public IUniform3i uniform3i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform3i> initializer)
    {
        return addUniform(new Uniform3i(name, initializer, frequency));
    }
    
    @Override
    public IUniform4i uniform4i(String name, @Nullable UniformUpdateFrequency frequency, @Nullable Consumer<IUniform4i> initializer)
    {
        return addUniform(new Uniform4i(name, initializer, frequency));
    }
    
    Program(PipelineVertexShader vertexShader, PipelineFragmentShader fragmentShader, TextureFormat textureFormat)
    {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.textureFormat = textureFormat;
    }
    
    @Override
    public TextureFormat textureFormat()
    {
        return this.textureFormat;
    }
    
    /**
     * Call after render / resource refresh to force shader reload.
     */
    public final void forceReload()
    {
        this.needsLoad = true;
    }
    
    @Override
    public IRenderPipeline finish()
    {
        this.isFinal = true;
        return this;
    }
    
    public void activate()
    {
        if(this.needsLoad)
            this.load();
        
        final int progID = this.progID;
        if(progID <= 0 || this.isErrored)
            return;
        
        OpenGlHelper.glUseProgram(progID);
        
        if(this.hasDirtyUniform)
        {
            this.hasDirtyUniform = false;
            this.uniforms.forEach(u -> u.upload());
        }
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
            this.hasDirtyUniform = true;
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
        
        Configurator.lightingModel.vertexFormat(this.textureFormat).bindAttributes(programID);
        
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
}