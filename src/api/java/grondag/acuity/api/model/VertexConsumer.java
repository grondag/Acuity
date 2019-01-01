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

package grondag.acuity.api.model;

import grondag.acuity.api.pipeline.RenderPipeline;

/**
 * Base interface for classes that will accept vertex data from models
 * and dynamic vertex producers. <p>
 * 
 * The AcceptVertex... methods are big and ugly for several reasons:<p><ol>
 * 
 * <li>Minimize call volume. These methods will definitely be in hot loops 
 * and HotSpot can only do so much.</li><p>
 * 
 * <li>Avoid confusion and bad input.  You call these methods and it just works. 
 * The main thing you can get wrong is to provide the wrong number of vertices.</li><p>
 * 
 * <li>Simplify implementation. Atomic vertex operations mean less internal state to track.</ol></li><p>
 * 
 * <b>General Notes</b><p>
 * 
 * You can mix pipelines, texture depth and all other settings as much as you please
 * for any given consumer.  The consumer will sort it all out and ensure your vertices are buffered
 * and rendered appropriately...<p>
 * 
 * However, texture depth must be the same for all vertices in a quad and if you assign a pipeline you 
 * must provide vertices that match the texture depth of the selected pipeline.<p>
 * 
 * If your quad vertices are not coplanar, split your quad and send as two triangles with a repeated vertex.
 * Lighting will not work well otherwise. May also break occlusion culling for block models.<p>
 * 
 * UV coordinates are expected to be fully baked, texture atlas pixel coordinates. (non-normalized)<br>
 * The API does not handle rotation, interpolation, or any of the things that normally happen during model bake.
 * The API also does not validate UV coordinates so that you can re-purpose them for custom shaders that 
 * don't need texture atlas coordinates.<p>
 * 
 * If your quad doesn't have vertex normals it is best to simply omit them, even if you have a face
 * normal already computed. The consumer will compute a face normal in any case.
 * This was done to reduce the number of logic paths, simplify the API and protect against bad inputs.<p>
 * 
 * For custom shaders that don't need/use vertex noThese methods will throw an exception if a custom shader isn't active.
 * Using raw vertices also consumes the bits that would normally be used to control lighting, MipMap and cutout blending.<p>
 * 
 * "Raw" methods will throw an exception if you have not enabled a custom pipeline via {@link #setPipeline(RenderPipeline)}.
 * This check prevent inadvertent use of this feature because default shaders will not render correctly when used.<p>
 */
public interface VertexConsumer
{
    /**
     * For single-layer renders with per-vertex normals.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int unlitColorARGB0,
            float u0,
            float v0);
   
    /**
     * For single-layer renders without per-vertex normals.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            int unlitColorARGB0,
            float u0,
            float v0);
    
    /**
     * For single-layer renders with custom normals and lighting data. Requires a custom pipeline.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptRawVertex(
            float posX,
            float posY,
            float posZ,
            int rawNormalData,
            int rawLightData,
            int unlitColorARGB0,
            float u0,
            float v0);
    
    /**
     * For double-layer renders with per-vertex normals.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1
            );
    
    
    /**
     * For double-layer renders without per-vertex normals.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1
            );
    
    /**
     * For double-layer renders with custom normals and lighting data. Requires a custom pipeline.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptRawVertex(
            float posX,
            float posY,
            float posZ,
            int rawNormalData,
            int rawLightData,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1
            );
    
    /**
     * For triple-layer renders with per-vertex normals.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1,
            int unlitColorARGB2,
            float u2,
            float v2
            );
    
    /**
     * For triple-layer renders without per-vertex normals.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1,
            int unlitColorARGB2,
            float u2,
            float v2
            );
    
    /**
     * For triple-layer renders with custom normals and lighting data. Requires a custom pipeline.<p>
     * 
     * See {@link VertexConsumer} header notes. 
     */
    public void acceptRawVertex(
            float posX,
            float posY,
            float posZ,
            int rawNormalData,
            int rawLightData,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1,
            int unlitColorARGB2,
            float u2,
            float v2
            );
    
    /**
     * True if the normal Minecraft lighting model is active.<p>
     * 
     * Use is not encouraged, but if you have lighting-dependent logic
     * you simply cannot avoid, here it is.<p>
     * 
     * Results will be identical to the RenderRuntime method of the same name.
     * Here as a convenience if you don't have any other reason to obtain
     * a runtime instance.
     */
    public boolean isStandardLightingModel();
    
    /**
     * Sets rendering pipeline to be used for the next completed quad.
     * If null, quad will use the default rendering pipeline for the texture
     * depth implied by the supplied vertices and transparency setting.<p>
     * 
     * If non-null, acceptVertex... method calls must match the pipeline texture depth.<p>
     * 
     * Will be null when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent quads until changed.<p>
     */
    public void setPipeline(RenderPipeline pipeline);
    
    /**
     * Enables or disables mip-mapping for the texture in the given layer.<p>
     * 
     * Mip-map is enabled by default. Disabling is useful for textures with sharp
     * edges or to prevent unwanted blending with background pixels.<p>
     * 
     * Applies to the next completed quad and remains in effect for all subsequent quads until changed.
     */
    public void enableMipMap(TextureDepth textureLayer, boolean enable);
    
    /**
     * Enables or disables cutout blending for the texture in the given layer.<p>
     * 
     * When true, texture pixels with alpha values < 0.5 will be discarded.
     * In the default multi-layer pipeline, this test is done before layers are blended,
     * so a cutout pixel on one layer has no effect on other layers.<p>
     * 
     * Cutout is disabled by default.<p>
     * 
     * Applies to the next completed quad and remains in effect for all subsequent quads until changed.
     */
    public void enableCutout(TextureDepth textureLayer, boolean enable);
    
    /**
     * Use this to control lighting of the texture in the given layer.
     * If enabled, the vertex light set by {@link #setVertexLight(int)} will be added
     * to world light for this texture layer. This result in full emissive rendering if 
     * vertex light is set to white (the default.)  However, more subtle lighting effects
     * are possible with other lightmap colors.<p>
     * 
     * Lighting occurs before texture blending if texture layers use different lighting.<p>
     * 
     * New value will apply to the next completed quad and remain in effect for all subsequent quads until changed.
     */
    public void enableVertexLight(TextureDepth textureLayer, boolean enable);
    
    /**
     * If enabled, textures lit by world light will have diffuse shading applied 
     * based on face/vertex normals.  In the standard lighting model shading is 
     * arbitrary (doesn't reflect movement of the sun, for example) but enhanced lighting
     * models will shade differently.
     * 
     * Enabled by default. Rarely used in world lighting but may be useful for some cutout
     * textures (vines, for example) that are pre-shaded or which render poorly with shading.<p>
     * 
     * New value will apply to the next completed quad and remain in effect for all subsequent quads until changed.
     */
    public void enableWorldLightDiffuse(boolean enable);
    
    /**
     * Works the same as {@link #enableWorldLightDiffuse(boolean)}, but applies
     * to surfaces lit by the provided vertex light color. (See {@link #enableVertexLight(TextureDepth, boolean)}.)<p>
     * 
     * Disabled by default.  Most textures with vertex lighting will be fully emissive
     * and it will not make sense to shade them. But this could be useful for partially 
     * illuminated surfaces. <p>
     * 
     * New value will apply to the next completed quad and remain in effect for all subsequent quads until changed.
     */
    public void enableVertexLightDiffuse(boolean enable);
    
    /**
     * If enabled, textures lit by world light will have ambient occlusion applied. 
     * 
     * Enabled by default and changes are rarely needed in world lighting.<p>
     * 
     * New value will apply to the next completed quad and remain in effect for all subsequent quads until changed.
     */
    public void enableWorldLightAmbientOcclusion(boolean enable);
    
    /**
     * Works the same as {@link #enableWorldLightAmbientOcclusion(boolean)}, but applies
     * to surfaces lit by the provided vertex light color. (See {@link #enableVertexLight(TextureDepth, boolean)}.)<p>
     * 
     * Disabled by default.  Most textures with vertex lighting will be fully emissive
     * and it will not make sense to shade them. But this could be useful for partially 
     * illuminated surfaces. <p>
     * 
     * New value will apply to the next completed quad and remain in effect for all subsequent quads until changed.
     */
    public void enableVertexLightAmbientOcclusion(boolean enable);
    
    /**
     * Accepts a light value in the form of an RGB color value to be used as the minimum block light
     * for the next <em>vertex</em>. The light will always be additive with other light sources.
     * Vertex lighting will be interpolated like any other vertex value.<p>
     * 
     * In the standard lighting model, this is converted into a monochromatic value based on luminance.
     * 
     * Vertex light is always <em>additive</em>.  For example, if you provide a dim blue vertex light and
     * your surface is next to a torch, your surface will render with mostly torch light (with flicker) but with a
     * boosted blue component. (Clamped to full white brightness.)  In direct sunlight dim vertex light probably
     * won't be noticeable.<p>
     * 
     * Will be full brightness (0xFFFFFF) when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent vertices until changed.<p>
     */
    public void setVertexLight(int lightRGB);
    
    /**
     * Version of {@link #setEmissiveLightMap(int)} that accepts unpacked int components.
     */
    public void setVertexLight(int red, int green, int blue);
    
    /**
     * Version of {@link #setEmissiveLightMap(int, int, int)} that accepts unpacked float components.
     */
    public void setVertexLight(float red, float green, float blue);
    
    /**
     * Returns all settings to default values.
     */
    public void clearSettings();
    
}
