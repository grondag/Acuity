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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormatElement.Format;

import static net.minecraft.client.render.VertexFormatElement.Format.*;

/**
 * Defines all vertex attributes to be used in API vertex bindings.<p>
 * 
 * Part of the public API to provide visibility and stability for custom shader authors.
 */
@Environment(EnvType.CLIENT)
public enum PipelineFormatElement
{
    // Note: OSX *really* wants to get vertex positions via standard (GL 2.1) binding. Slows to a crawl otherwise.
    POSITION_3F(FLOAT, 3, "in_pos", false),
    BASE_RGBA_4UB(UNSIGNED_BYTE, 4, "in_color_0", false),
    BASE_TEX_2F(FLOAT, 2, "in_uv_0", false),

    /**
     * Format varies by model.  <p>
     * 
     * In vanilla lighting model, Bytes 1-2 are sky and block lightmap coordinates<br>
     * 3rd byte is has control flags.  bits 0-2 are emissive flags, bit 3 controls mimmaping (1=off), bit 4 is cutout<br>
     * bits 5-7 and the last byte are reserved
     * 
     * In enhanced lighting model, bytes 1-3 are rgb light color/glow  flag, and the last byte is amount of torch flicker.
     * The most significant bit of the rgb color bytes indicates if layers are emissive. 
     * The color values are thus scale to 0-127 and need to be normalized in the shader after stripping the glow bit.<p>
     */
    LIGHTMAPS_4UB(UNSIGNED_BYTE, 4, "in_lightmap", false),

    NORMAL_AO_4UB(UNSIGNED_BYTE, 4, "in_normal_ao", false),

    SECONDARY_RGBA_4UB(UNSIGNED_BYTE, 4, "in_color_1", false),
    SECONDARY_TEX_2F(FLOAT, 2, "in_uv_1", false),

    TERTIARY_RGBA_4UB(UNSIGNED_BYTE, 4, "in_color_2", false),
    TERTIARY_TEX_2F(FLOAT, 2, "in_uv_2", false);

    /** Identifies attribute in vertex bindings. */
    public final String glName;
    
    /** GL data type constant for vertex bindings. */
    public final int glConstant;
    
    /** If > 1, means this element is a vector. */
    public final int elementCount;
    
    /** Applies to float types. If true, means values are normalized to unit range. */
    public final boolean isNormalized;
    
    /** Stride of this element, in bytes. */
    public final int byteSize;
    
    private PipelineFormatElement(Format format, int elementCount, String glName, boolean isNormalized)
    {
        this.glName = glName;
        this.elementCount = elementCount;
        this.glConstant = format.getGlId();
        this.byteSize = format.getSize() * elementCount;
        this.isNormalized = isNormalized;
    }
}
