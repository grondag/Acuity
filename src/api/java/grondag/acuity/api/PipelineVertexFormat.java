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

package grondag.acuity.api;

import static grondag.acuity.api.PipelineFormatElement.BASE_RGBA_4UB;
import static grondag.acuity.api.PipelineFormatElement.BASE_TEX_2F;
import static grondag.acuity.api.PipelineFormatElement.LIGHTMAPS_4UB;
import static grondag.acuity.api.PipelineFormatElement.NORMAL_AO_4UB;
import static grondag.acuity.api.PipelineFormatElement.POSITION_3F;
import static grondag.acuity.api.PipelineFormatElement.SECONDARY_RGBA_4UB;
import static grondag.acuity.api.PipelineFormatElement.SECONDARY_TEX_2F;
import static grondag.acuity.api.PipelineFormatElement.TERTIARY_RGBA_4UB;
import static grondag.acuity.api.PipelineFormatElement.TERTIARY_TEX_2F;

import org.lwjgl.opengl.GL20;

import grondag.acuity.opengl.OpenGlHelperExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormat;

@Environment(EnvType.CLIENT)
public enum PipelineVertexFormat
{
    VANILLA_SINGLE(0, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(LIGHTMAPS_4UB)),
    
    /**
     * Adds one extra color and texture coordinate.
     * Use for two-layered textures.
     */
    VANILLA_DOUBLE(1, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)),
    
    /**
     * Adds two extra colors and texture coordinates.
     * Use for three-layered materials.
     */
    VANILLA_TRIPLE(2, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)
            .add(TERTIARY_RGBA_4UB)
            .add(TERTIARY_TEX_2F)),
    
    ENHANCED_SINGLE(0, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(NORMAL_AO_4UB)
            .add(LIGHTMAPS_4UB)),
    
    ENHANCED_DOUBLE(1, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(NORMAL_AO_4UB)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)),
    
    ENHANCED_TRIPLE(2, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(NORMAL_AO_4UB)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)
            .add(TERTIARY_RGBA_4UB)
            .add(TERTIARY_TEX_2F));
    
    public final VertexFormat vertexFormat;
    
    /**
     * Will be a unique, 0-based ordinal within the current lighting model.
     */
    public final int layerIndex;
    
    public final int attributeCount;
    public final int stride;
    
    private final PipelineFormatElement [] elements;
    
    private  PipelineVertexFormat(int layerIndex, VertexFormat vertexFormat)
    {
        this.layerIndex = layerIndex;
        this.vertexFormat = vertexFormat;
        this.stride = vertexFormat.getVertexSize();
        this.elements = vertexFormat.getElements().toArray(new PipelineFormatElement[vertexFormat.getElementCount()]);
        int count = 0;
        for(PipelineFormatElement e : elements)
        {
            if(e != POSITION_3F)
                count++;
        }
        this.attributeCount = count;
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
        for(PipelineFormatElement e : elements)
        {
            if(e != POSITION_3F)
                GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, stride, bufferOffset + offset);
            offset += e.byteSize;
        }
    }
    
    public void bindProgramAttributes(int  programID)
    {
        int index = 1;
        for(PipelineFormatElement e : elements)
        {
            if(e != POSITION_3F)
                GL20.glBindAttribLocation(programID, index++, e.glName);
        }
    }
}
