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

import grondag.acuity.api.model.TextureDepth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface PipelineManager
{
    /**
     * Will return null if pipeline limit would be exceeded.
     */
    RenderPipeline createPipeline(TextureDepth textureFormat, String vertexShader, String fragmentShader);  
    
    /**
     * Use when you want standard rendering.
     */
    RenderPipeline getDefaultPipeline(TextureDepth textureFormat);

    RenderPipeline getWaterPipeline();

    RenderPipeline getLavaPipeline();

    /**
     * See {@link RenderPipeline#getIndex()}
     */
    RenderPipeline getPipelineByIndex(int index);

    /**
     * The number of seconds this world has been rendering since the last render reload,
     * including fractional seconds. Based on total world time, but shifted to 
     * originate from start of this game session. <p>
     * 
     * Use if you somehow need to know what world time is being sent to shader uniforms.
     */
    float renderSeconds();
    
}
