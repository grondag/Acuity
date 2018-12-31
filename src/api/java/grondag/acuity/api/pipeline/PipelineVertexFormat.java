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

import static grondag.acuity.api.pipeline.PipelineVertexAttribute.BASE_RGBA_4UB;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.BASE_TEX_2F;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.LIGHTMAPS_4UB;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.NORMAL_BLEND_4UB;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.POSITION_3F;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.SECONDARY_RGBA_4UB;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.SECONDARY_TEX_2F;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.TERTIARY_RGBA_4UB;
import static grondag.acuity.api.pipeline.PipelineVertexAttribute.TERTIARY_TEX_2F;

import org.lwjgl.opengl.GL20;

import grondag.acuity.opengl.OpenGlHelperExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum PipelineVertexFormat
{
    /**
     * Minimal format, used for single-layer blocks and items.<p>
     * 
     * Four bytes longer than standard Minecraft format due to inclusion of normals.
     * We accept this slight overhead to support enhanced lighting and provide consistency 
     * across lighting models for custom shaders.
     */
    SINGLE( POSITION_3F,
            BASE_RGBA_4UB,
            BASE_TEX_2F,
            NORMAL_BLEND_4UB,
            LIGHTMAPS_4UB),
    
    /**
     * Adds one extra color and texture coordinate.<br>
     * Used for two-layered blocks and items.
     */
    DOUBLE( POSITION_3F,
            BASE_RGBA_4UB,
            BASE_TEX_2F,
            NORMAL_BLEND_4UB,
            LIGHTMAPS_4UB,
            SECONDARY_RGBA_4UB,
            SECONDARY_TEX_2F),
    
    /**
     * Adds two extra colors and texture coordinates.<br>
     * Used for three-layered blocks and items.
     */
    TRIPLE( POSITION_3F,
            BASE_RGBA_4UB,
            BASE_TEX_2F,
            NORMAL_BLEND_4UB,
            LIGHTMAPS_4UB,
            SECONDARY_RGBA_4UB,
            SECONDARY_TEX_2F,
            TERTIARY_RGBA_4UB,
            TERTIARY_TEX_2F);
    
    public final int attributeCount;
    public final int stride;
    
    final PipelineVertexAttribute [] elements;
    
    private  PipelineVertexFormat(PipelineVertexAttribute... elements)
    {
        this.elements = elements;
        int count = 0;
        int stride = 0;
        for(PipelineVertexAttribute e : elements)
        {
            stride += e.byteSize;
            if(e != POSITION_3F)
                count++;
        }
        this.attributeCount = count;
        this.stride = stride;
    }
    
    /**
     * Enables generic vertex attributes and binds their location.
     */
    public void enableAndBindAttributes(int bufferOffset)
    {
        OpenGlHelperExt.enableAttributes(this.attributeCount);
        bindAttributeLocations(bufferOffset);
    }
    
    /**
     * Binds attribute locations without enabling them. 
     * For use with VAOs. In other cases just call {@link #enableAndBindAttributes(int)}
     */
    public void bindAttributeLocations(int bufferOffset)
    {
        int offset = 0;
        int index = 1;
        for(PipelineVertexAttribute e : elements)
        {
            if(e != POSITION_3F)
                GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, stride, bufferOffset + offset);
            offset += e.byteSize;
        }
    }
    
    public void bindProgramAttributes(int  programID)
    {
        int index = 1;
        for(PipelineVertexAttribute e : elements)
        {
            if(e != POSITION_3F)
                GL20.glBindAttribLocation(programID, index++, e.glName);
        }
    }
}
