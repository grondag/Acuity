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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.Matrix4f;

/**
 * Defines the methods used to refresh uniform values.  
 * You need these to construct consumers for uniform initialization.
 * There is no need to have a reference to these - they will only 
 * be used inside the consumers you provide when uniforms are created.
 */
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
