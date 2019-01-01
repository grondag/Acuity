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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GLX;

import grondag.acuity.Acuity;
import grondag.acuity.api.model.TextureDepth;
import grondag.acuity.api.pipeline.PipelineUniform;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform1f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform1i;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform2f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform2i;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform3f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform3i;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform4f;
import grondag.acuity.api.pipeline.PipelineUniform.Uniform4i;
import grondag.acuity.api.pipeline.PipelineUniform.UniformMatrix4f;
import grondag.acuity.api.pipeline.UniformUpdateFrequency;
import grondag.acuity.fermion.config.Localization;
import grondag.acuity.mixin.extension.Matrix4fExt;
import grondag.acuity.opengl.OpenGlHelperExt;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.Matrix4f;

@Environment(EnvType.CLIENT)
public class Program
{
    private static Program activeProgram;
    
    public static void deactivate()
    {
        activeProgram = null;
        GLX.glUseProgram(0);
    }
    
    private int progID = -1;
    private boolean isErrored = false;

    public final PipelineVertexShader vertexShader;
    public final PipelineFragmentShader fragmentShader;
    public final TextureDepth textureDepth;
    public final boolean isSolidLayer;
 
    private final ObjectArrayList<Uniform<?>> uniforms = new ObjectArrayList<>();
    private final ObjectArrayList<Uniform<?>> renderTickUpdates = new ObjectArrayList<>();
    private final ObjectArrayList<Uniform<?>> gameTickUpdates = new ObjectArrayList<>();
    
    protected int dirtyCount = 0;
    protected final Uniform<?>[] dirtyUniforms = new Uniform[32];
    
    /**
     * Tracks last matrix version to avoid unnecessary uploads.
     */
    private int lastViewMatrixVersion = 0;
    
    public abstract class Uniform<T extends PipelineUniform>
    {
        protected static final int FLAG_NEEDS_UPLOAD = 1;
        protected static final int FLAG_NEEDS_INITIALIZATION = 2;
        
        private final String name;
        protected int flags = 0;
        protected int unifID = -1;
        protected final Consumer<T> initializer;
        protected final UniformUpdateFrequency frequency;
        
        protected Uniform(String name, Consumer<T> initializer, UniformUpdateFrequency frequency)
        {
            this.name = name;
            this.initializer = initializer;
            this.frequency = frequency;
        }
        
        public final void setDirty()
        {
            final int flags = this.flags;
            if(flags == 0)
                dirtyUniforms[dirtyCount++] = this;
            
            this.flags = flags | FLAG_NEEDS_UPLOAD;
        }
        
        protected final void markForInitialization()
        {
            final int flags = this.flags;
            
            if(flags == 0)
                dirtyUniforms[dirtyCount++] = this;
            
            this.flags = flags | FLAG_NEEDS_INITIALIZATION;
        }
        
        private final void load(int programID)
        {
            this.unifID = GLX.glGetUniformLocation(programID, name);
            if(this.unifID == -1)
            {
                Acuity.INSTANCE.getLog().debug(Localization.translate("misc.debug_missing_uniform", name, Program.this.vertexShader.fileName, Program.this.fragmentShader.fileName));
                this.flags = 0;
            }
            else
            {
                // never add view uniforms to dirty list - have special handling
                if(this == modelViewUniform || this == modelViewProjectionUniform)
                    this.flags = 0;
                else
                {
                    // dirty count will be reset to 0 before uniforms are loaded
                    dirtyUniforms[dirtyCount++] = this;
                    this.flags = FLAG_NEEDS_INITIALIZATION | FLAG_NEEDS_UPLOAD;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        protected final void upload()
        {
            if(this.flags == 0)
                return;
            
            if((this.flags & FLAG_NEEDS_INITIALIZATION) == FLAG_NEEDS_INITIALIZATION)
                this.initializer.accept((T) this);
            
            if((this.flags & FLAG_NEEDS_UPLOAD) == FLAG_NEEDS_UPLOAD)
                this.uploadInner();
            
            this.flags = 0;
        }
        
        protected abstract void uploadInner();
    }
    
    protected abstract class UniformFloat<T extends PipelineUniform> extends Uniform<T>
    {
        protected final FloatBuffer uniformFloatBuffer;
        
        protected UniformFloat(String name, Consumer<T> initializer, UniformUpdateFrequency frequency, int size)
        {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = BufferUtils.createFloatBuffer(size);
        }
    }
    
    public class Uniform1fImpl extends UniformFloat<Uniform1f> implements Uniform1f
    {
        protected Uniform1fImpl(String name, Consumer<Uniform1f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 1);
        }

        @Override
        public final void set(float value)
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
            GLX.glUniform1(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    public class Uniform2fImpl extends UniformFloat<Uniform2f> implements Uniform2f
    {
        protected Uniform2fImpl(String name, Consumer<Uniform2f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 2);
        }

        @Override
        public final void set(float v0, float v1)
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
            GLX.glUniform2(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    public class Uniform3fImpl extends UniformFloat<Uniform3f> implements Uniform3f
    {
        protected Uniform3fImpl(String name, Consumer<Uniform3f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 3);
        }

        @Override
        public final void set(float v0, float v1, float v2)
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
            GLX.glUniform3(this.unifID, this.uniformFloatBuffer);
       }
    }
    
    public class Uniform4fImpl extends UniformFloat<Uniform4f> implements Uniform4f
    {
        protected Uniform4fImpl(String name, Consumer<Uniform4f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 4);
        }

        @Override
        public final void set(float v0, float v1, float v2, float v3)
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
            GLX.glUniform4(this.unifID, this.uniformFloatBuffer);
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
    
    public Uniform1f uniform1f(String name, UniformUpdateFrequency frequency, Consumer<Uniform1f> initializer)
    {
        return addUniform(new Uniform1fImpl(name, initializer, frequency));
    }
    
    public Uniform2f uniform2f(String name, UniformUpdateFrequency frequency, Consumer<Uniform2f> initializer)
    {
        return addUniform(new Uniform2fImpl(name, initializer, frequency));
    }
    
    public Uniform3f uniform3f(String name, UniformUpdateFrequency frequency, Consumer<Uniform3f> initializer)
    {
        return addUniform(new Uniform3fImpl(name, initializer, frequency));
    }
    
    public Uniform4f uniform4f(String name, UniformUpdateFrequency frequency, Consumer<Uniform4f> initializer)
    {
        return addUniform(new Uniform4fImpl(name, initializer, frequency));
    }
    
    protected abstract class UniformInt<T extends PipelineUniform> extends Uniform<T>
    {
        protected final IntBuffer uniformIntBuffer;
        
        protected UniformInt(String name, Consumer<T> initializer, UniformUpdateFrequency frequency, int size)
        {
            super(name, initializer, frequency);
            this.uniformIntBuffer = BufferUtils.createIntBuffer(size);
        }
    }
    
    public class Uniform1iImpl extends UniformInt<Uniform1i> implements Uniform1i
    {
        protected Uniform1iImpl(String name, Consumer<Uniform1i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 1);
        }

        @Override
        public final void set(int value)
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
            GLX.glUniform1(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform2iImpl extends UniformInt<Uniform2i> implements Uniform2i
    {
        protected Uniform2iImpl(String name, Consumer<Uniform2i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 2);
        }

        @Override
        public final void set(int v0, int v1)
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
            GLX.glUniform2(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform3iImpl extends UniformInt<Uniform3i> implements Uniform3i
    {
        protected Uniform3iImpl(String name, Consumer<Uniform3i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 3);
        }

        @Override
        public final void set(int v0, int v1, int v2)
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
            GLX.glUniform3(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform4iImpl extends UniformInt<Uniform4i> implements Uniform4i
    {
        protected Uniform4iImpl(String name, Consumer<Uniform4i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 4);
        }

        @Override
        public final void set(int v0, int v1, int v2, int v3)
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
            GLX.glUniform4(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public Uniform1i uniform1i(String name, UniformUpdateFrequency frequency, Consumer<Uniform1i> initializer)
    {
        return addUniform(new Uniform1iImpl(name, initializer, frequency));
    }
    
    public Uniform2i uniform2i(String name, UniformUpdateFrequency frequency, Consumer<Uniform2i> initializer)
    {
        return addUniform(new Uniform2iImpl(name, initializer, frequency));
    }
    
    public Uniform3i uniform3i(String name, UniformUpdateFrequency frequency, Consumer<Uniform3i> initializer)
    {
        return addUniform(new Uniform3iImpl(name, initializer, frequency));
    }
    
    public Uniform4i uniform4i(String name, UniformUpdateFrequency frequency, Consumer<Uniform4i> initializer)
    {
        return addUniform(new Uniform4iImpl(name, initializer, frequency));
    }
    
    public Program(PipelineVertexShader vertexShader, PipelineFragmentShader fragmentShader, TextureDepth textureFormat, boolean isSolidLayer)
    {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.textureDepth = textureFormat;
        this.isSolidLayer = isSolidLayer;
    }
 
    /**
     * Call after render / resource refresh to force shader reload.
     */
    public final void forceReload()
    {
        this.load();
    }
    
    /**
     * Handle these directly because may update each activation.
     */
    private final void updateModelUniforms()
    {
        if(lastViewMatrixVersion != PipelineManagerImpl.viewMatrixVersionCounter)
        {
            updateModelUniformsInner();
            lastViewMatrixVersion = PipelineManagerImpl.viewMatrixVersionCounter;
        }
    }
    
    private final void updateModelUniformsInner()
    {
        if(this.modelViewUniform != null);
            this.modelViewUniform.uploadInner();
    
        if(this.modelViewProjectionUniform != null);
            this.modelViewProjectionUniform.uploadInner();
    }
    
    public final void activate()
    {
        if(this.isErrored)
            return;
        
        if(activeProgram != this)
        {
            activeProgram = this;
            GLX.glUseProgram(this.progID);
    
            final int count = this.dirtyCount;
            if(count != 0)
            {            
                for(int i = 0; i < count; i++)
                {
                    this.dirtyUniforms[i].upload();
                }
                this.dirtyCount = 0;
            }
        }
        
        this.updateModelUniforms();
    }
    
    public class UniformMatrix4fImpl extends Uniform<UniformMatrix4f> implements UniformMatrix4f
    {
        protected final FloatBuffer uniformFloatBuffer;
        protected final long bufferAddress;
        
        protected final float[] lastValue = new float[16];
        
        protected UniformMatrix4fImpl(String name, Consumer<UniformMatrix4f> initializer, UniformUpdateFrequency frequency)
        {
            this(name, initializer, frequency, BufferUtils.createFloatBuffer(16));
        }

        /**
         * Use when have a shared direct buffer
         */
        protected UniformMatrix4fImpl(String name, Consumer<UniformMatrix4f> initializer, UniformUpdateFrequency frequency, FloatBuffer uniformFloatBuffer)
        {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = uniformFloatBuffer;
            this.bufferAddress = MemoryUtil.memAddress(this.uniformFloatBuffer);
        }
        
        @Override
        public final void set(Matrix4f matrix)
        {
            this.set(((Matrix4fExt)(Object)matrix).getComponents());
        }
        
        @Override
        public final void set(float... elements)
        {
            if(this.unifID == -1) return;
            if(elements.length != 16)
                throw new ArrayIndexOutOfBoundsException("Matrix arrays must have 16 elements");
            
            if(lastValue[0] == elements[0]
                 && lastValue[1] == elements[1]
                 && lastValue[2] == elements[2]
                 && lastValue[3] == elements[3]
                 && lastValue[4] == elements[4]
                 && lastValue[5] == elements[5]
                 && lastValue[6] == elements[6]
                 && lastValue[7] == elements[7]
                 && lastValue[8] == elements[8]
                 && lastValue[9] == elements[9]
                 && lastValue[10] == elements[10]
                 && lastValue[11] == elements[11]
                 && lastValue[12] == elements[12]
                 && lastValue[13] == elements[13]
                 && lastValue[14] == elements[14]
                 && lastValue[15] == elements[15]) 
                return;
            
            // avoid NIO overhead
            if(OpenGlHelperExt.isFastNioCopyEnabled())
                OpenGlHelperExt.fastMatrix4fBufferCopy(elements, bufferAddress);
            else
            {
                this.uniformFloatBuffer.put(elements, 0, 16);
                this.uniformFloatBuffer.position(0);
            }
           
            this.setDirty();
        }
        
        @Override
        protected void uploadInner()
        {
            GLX.glUniformMatrix4(this.unifID, true, this.uniformFloatBuffer);
        }
    }
    
    public UniformMatrix4fImpl uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<UniformMatrix4f> initializer)
    {
        return addUniform(new UniformMatrix4fImpl(name, initializer, frequency));
    }
    
    public UniformMatrix4fImpl uniformMatrix4f(String name, UniformUpdateFrequency frequency, FloatBuffer floatBuffer, Consumer<UniformMatrix4f> initializer)
    {
        return addUniform(new UniformMatrix4fImpl(name, initializer, frequency, floatBuffer));
    }
    
    private final void load()
    {
        this.isErrored = true;
        
        // prevent accumulation of uniforms in programs that aren't activated after multiple reloads
        this.dirtyCount = 0;
        try
        {
            if(this.progID > 0)
                GLX.glDeleteProgram(progID);
            
            this.progID = GLX.glCreateProgram();
            
            this.isErrored = this.progID > 0 && !loadInner();
        }
        catch(Exception e)
        {
            if(this.progID > 0)
                GLX.glDeleteProgram(progID);
            
            Acuity.INSTANCE.getLog().error(Localization.translate("misc.error_program_link_failure"), e);
            this.progID = -1;
        }
        
        if(!this.isErrored)
        {   
            final int limit = uniforms.size();
            for(int i = 0; i < limit; i++)
                uniforms.get(i).load(progID);
        }
        
    }
    
    /**
     * Return true on success
     */
    private final boolean loadInner()
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
        
        GLX.glAttachShader(programID, vertId);
        GLX.glAttachShader(programID, fragId);
        
        textureDepth.vertexFormat.bindProgramAttributes(programID);
        
        GLX.glLinkProgram(programID);
        if(GLX.glGetProgrami(programID, GLX.GL_LINK_STATUS) == GL11.GL_FALSE)
        {
            Acuity.INSTANCE.getLog().error(OpenGlHelperExt.getProgramInfoLog(programID));
            return false;
        }
        
        return true;
    }

    public final void onRenderTick()
    {
        final int limit = renderTickUpdates.size();
        for(int i = 0; i < limit; i++)
        {
            renderTickUpdates.get(i).markForInitialization();
        }
    }

    public final void onGameTick()
    {
        final int limit = gameTickUpdates.size();
        for(int i = 0; i < limit; i++)
        {
            gameTickUpdates.get(i).markForInitialization();
        }
    }
    
    public UniformMatrix4fImpl modelViewUniform;
    public UniformMatrix4fImpl modelViewProjectionUniform;
    public UniformMatrix4fImpl projectionMatrixUniform;
    
    public final void setupModelViewUniforms()
    {
        if(containsUniformSpec("mat4", "u_modelView"))
        {
            this.modelViewUniform = this.uniformMatrix4f("u_modelView", UniformUpdateFrequency.ON_LOAD, 
                    PipelineManagerImpl.modelViewMatrixBuffer, u -> 
            {
                this.modelViewUniform.setDirty();
            });
        }

        if(containsUniformSpec("mat4", "u_modelViewProjection"))
        {
            this.modelViewProjectionUniform = this.uniformMatrix4f("u_modelViewProjection", UniformUpdateFrequency.ON_LOAD,
                    PipelineManagerImpl.modelViewProjectionMatrixBuffer, u -> 
            {
                this.modelViewProjectionUniform.setDirty();
            });
        }
        
        if(containsUniformSpec("mat4", "u_projection"))
        {
            // on load because handled directly
            this.projectionMatrixUniform = this.uniformMatrix4f("u_projection", UniformUpdateFrequency.ON_LOAD, 
                    PipelineManagerImpl.projectionMatrixBuffer, u -> 
            {
                this.projectionMatrixUniform.setDirty();
            });
        }
        
        
    }
    
    public boolean containsUniformSpec(String type, String name)
    {
        String regex = "(?m)^uniform\\s+" + type + "\\s+" + name + "\\s*;";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(this.vertexShader.getSource()).find() 
                || pattern.matcher(this.fragmentShader.getSource()).find(); 
    }
}
