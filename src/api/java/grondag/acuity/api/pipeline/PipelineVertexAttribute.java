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
import net.minecraft.client.render.VertexFormatElement.Format;

import static net.minecraft.client.render.VertexFormatElement.Format.*;

import grondag.acuity.api.model.VertexConsumer;

/**
 * Defines all vertex attributes to be used in API vertex bindings.<p>
 * 
 * Part of the public API to provide visibility and stability for custom shader authors.
 */
@Environment(EnvType.CLIENT)
public enum PipelineVertexAttribute
{
    /**
     * Position.  Will always be present and will always be first.<p>
     * 
     * Note: OSX *really* wants to get vertex positions via glPosition (GL 2.1 / GLSL 120) binding. 
     * Slows to a crawl otherwise.
     */
    POSITION_3F(FLOAT, 3, "in_pos", false),

    /**
     * In standard lighting model, two high bytes are standard sky and block lightmap coordinates<br>
     * 3rd byte is monochrome custom lightmap. Low byte holds lighting mode for each render layer.<p> 
     * 
     * In enhanced lighting model, most of this will probably be dedicated to a color lightmap, 
     * with the low byte still controlling per-layer lighting mode. <br>
     * Above assumes world/block lighting will be done in GPU and not require per-vertex data.<p>
     * 
     * If you don't need standard lighting data in your shader, use
     * {@link VertexConsumer#setCustomLightData(int)} to pipe your data to the shader.
     */
    LIGHTMAPS_4UB(UNSIGNED_BYTE, 4, "in_lightmap", false),

    /**
     * Three high bytes contain low-precision vertex normals packed as bytes.<br>
     * Vertex normals aren't needed in vanilla lighting model, but always sent to 
     * provide consistency for custom shaders across lighting models.
     * If you do not provide vertex normals these will be computed face normals. <p>
     * 
     * Low byte contains bit flags to control cutout and mimmap for standard renders.
     * Ideally these flags would be sent 1x per quad but limited to GL2.1 / GLSL 120...<p>
     * 
     * If you don't need normals or blend flags in your custom shader, use
     * {@link VertexConsumer#setCustomNormalBlendData(int)} to pass your custom data.
     */
    NORMAL_BLEND_4UB(UNSIGNED_BYTE, 4, "in_normal_blend", false),
    
    /**
     * Base layer color.  What you get in shader will be different than what you send to
     * vertex consumer if the active lighting model does any shading outside the GPU.
     * If your shader needs raw data, enable the emissive or raw vertex light source.
     */
    BASE_RGBA_4UB(UNSIGNED_BYTE, 4, "in_color_0", false),
    
    /**
     * Base layer UV coordinates.  What you get in shader will always be what you send to vertex consumer.
     * Standard shaders require these to be texture atlas coordinates, but vertex consumer doesn't check.
     * Thus, you could use these for other purposes in a custom shader.<p>
     * 
     * Future versions might use compressed UV coordinates for atlas textures, if that proves to be viable.
     */
    BASE_TEX_2F(FLOAT, 2, "in_uv_0", false),
    
    /** Same as {@link #BASE_RGBA_4UB} for second texture layer. Only present when texture depth >= 2 */
    SECONDARY_RGBA_4UB(UNSIGNED_BYTE, 4, "in_color_1", false),
    
    /** Same as {@link #BASE_TEX_2F} for second texture layer. Only present when texture depth >= 2 */
    SECONDARY_TEX_2F(FLOAT, 2, "in_uv_1", false),

    /** Same as {@link #BASE_RGBA_4UB} for third texture layer. Only present when texture depth == 3 */
    TERTIARY_RGBA_4UB(UNSIGNED_BYTE, 4, "in_color_2", false),
    
    /** Same as {@link #BASE_TEX_2F} for third texture layer. Only present when texture depth == 3 */
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
    
    private PipelineVertexAttribute(Format format, int elementCount, String glName, boolean isNormalized)
    {
        this.glName = glName;
        this.elementCount = elementCount;
        this.glConstant = format.getGlId();
        this.byteSize = format.getSize() * elementCount;
        this.isNormalized = isNormalized;
    }
}
