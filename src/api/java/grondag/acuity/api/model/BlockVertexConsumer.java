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

import java.util.Random;

import grondag.acuity.api.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

/**
 * Block models will call {@link IPipelinedVertexConsumer#acceptVertex(IPipelinedVertex)} with vertex information.<p>
 * 
 * For tint, quad (or the model it comes from) is responsible for retrieving and applying block tint 
 * to the vertex colors.  This is done because lighter has no way to know which colors
 * should be modified when there is more than one color/texture layer. And many models don't use it.<p>
 * 
 * You can retrieve the block color from tint with {@link IPipelinedVertexConsumer#getBlockInfo()} 
 * and then {@link BlockInfo#getColorMultiplier(int tint)};
 */
@Environment(EnvType.CLIENT)
public interface BlockVertexConsumer extends VertexConsumer
{
    /**
     * Provides access to in-world block position for model customization.
     */
    public BlockPos pos();
    
    /**
     * Provides access to block world for model customization.
     */
    public ExtendedBlockView world();
    
    /**
     * Provides access to block state for model customization.<br>
     * Is what normally is passed to IBakedModel and may be an IExtendedBlockState.
     */
    public BlockState blockState();
    
    /**
     * Deterministically pseudo-random based on block position.<br>
     * Will be same as what normally is passed to BakedModel but is initialized
     * lazily - will not be initialized if never retrieved.
     */
    public Random random();
    
    /**
     * True if the normal Minecraft lighting model is active.
     */
    public boolean isVanillaLightingModel();
    
    /**
     * If your model has an efficient way to exclude quads that are co-planar 
     * with a block face then you should check this method and skip quads for
     * sides when this method returns false. <p>
     * 
     * If you are using this check, then you should also call {@link #setAutomaticOcclusionFilter()}.
     * to disable automatic quad occlusion filtering.<p>
     * 
     * This method is only meaningful for in-world block rendering and will always return true for item rendering
     * and other rendering scenarios where face occlusion doesn't apply.
     */
    public boolean shouldDrawSide(Direction side);
    
    /**
     * Sets rendering pipeline to be used for the next completed quad.
     * If null, quad will use the default rendering pipeline for the texture
     * depth implied by the supplied vertices and blend mode.<p>
     * 
     * If non-null, acceptVertex method calls must match the pipeline texture depth.<p>
     * 
     * Will be null when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent quads until changed.<p>
     */
    public void setPipeline(RenderPipeline pipeline);
    
    /**
     * By default, when rendering blocks in the world, the consumer will determine
     * if incoming quads are coplanar with a block face and ignore the quad if that face isn't visible.<p>
     * 
     * If you are checking {@link #shouldDrawSide(Direction)} or if your model contains only
     * quads that are not coplanar, you can improve performance slightly by turning this feature off.<p>
     * 
     * This setting is only meaningful for in-world block rendering and the feature will always
     * be disabled for item rendering and other rendering scenarios where face occlusion doesn't apply.<p>
     * 
     * Will be true when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent quads/vertices until changed.<p>
     */
    public void setAutomaticOcclusionFilter(boolean isEnabled);
    
    /**
     * Use this to enable or disable mip-mapping for the texture in the given layer.<p>
     * 
     * Mip-map is enabled by default. Disabling is useful for textures with sharp
     * edges or to prevent unwanted blending with background pixels.<p>
     * 
     * Applies to the next quad and remains in effect for all subsequent quads until changed.
     */
    public void setMipMap(TextureDepth textureLayer, boolean isMimMapEnabled);
    
    /**
     * Use this to render the texture in the given layer with additive lighting.<p>
     * 
     * The lightmap for emissive rendering is specified via {@link #setEmissiveLightMap(int)}
     * and default to full brightness.<p>
     * 
     * Enabling this will disable shading and ambient occlusion for the given texture layer.<p>
     * 
     * Applies to the next <em>vertex</em> and remains in effect for all subsequent vertices until changed.
     */
    public void setEmissive(TextureDepth textureLayer, boolean isEmissive);
    
    public static final int FULL_BRIGHTNESS_NO_FLICKER = 0xFFFFFF00;
    
    /**
     * Accepts a lightmap in the form of an RGBA color value to be used as the minimum block light
     * for the next emissive <em>vertex</em>. The lightmap will always be additive with other light sources.  
     * The alpha "F" value indicates how much of this light is from torches and 
     * thus modified by torch flickering. Zero means no flicker.<p>
     * 
     * In the vanilla lighting model, this is converted into a single 0-255 (mostly monochromatic) 
     * block lightmap value that works just like vanilla block lightmaps. (Conversion is based on luminance.)
     * The light will be torch light, and will always have 100% torch flicker. (Your flicker value is ignored.) 
     * The value is a <em>minimum</em>, so vertices that are not full brightness in your lightmap
     * can still be lit at full brightness if in sunlight or if next to a light source.  (Just like vanilla)<p>
     * 
     * In the enhanced lighting model, your lightmap will be rendered in color, and will include some amount of
     * flicker if you provide a non-zero flicker value.  <p>
     * 
     * Your lightmap is always <em>additive</em>.  For example, if you provide a dim blue lightmap and
     * your surface is next to a torch, your surface will render with mostly torch light (with flicker) but with a
     * boosted blue component. (Clamped to full white brightness.)  In direct sunlight your lightmap probably
     * won't be noticeable.<p>
     * 
     * Will be {@link #FULL_BRIGHTNESS_NO_FLICKER} when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent vertices until changed.<p>
     * 
     */
    public void setEmissiveLightMap(int lightRGBF);
    
    /**
     * Version of {@link #setEmissiveLightMap(int, int, int, int)} that accepts unpacked int components.
     */
    public void setEmissiveLightMap(int red, int green, int blue, int flicker);
    
    /**
     * Version of {@link #setEmissiveLightMap(int, int, int, int)} that accepts unpacked float components.
     */
    public void setEmissiveLightMap(float red, float green, float blue, float flicker);
    
    /**
     * Controls how the next quad will be blended with the scene background.<br>
     * 
     * Will be {@link BlendMode#SOLID} when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent quads until changed.<p>
     * 
     * See BlendMode for details on the various modes.<br>
     */
    public void setBlendMode(BlendMode blendMode);
    
    /**
     * TODO: finish - still needed? Should it/can it be by layer?
     * Can be used to enable or disable the diffuse shading component of ambient lighting. <br>
     * Applies to all non-emissive texture layers.<p>
     * 
     * Disabling does NOT disable lighting (light maps or future illumination models still apply).  
     * To disable lighting, (and render surface full brightness) use {@link #setEmissive(int, boolean)} <p>
     * 
     * The effect of disabling diffuse shading depends on the lighting model...<p>
     * 
     * In the vanilla lighting model, ambient shading is arbitrary, and does not consider light direction.
     * It simply serves to make block faces look distinct.  You will want to disable it when you
     * are providing a pre-shaded quad. <p>
     * 
     * Note that pre-shaded quads are not recommended because they will not render well in future
     * lighting models. If you must, check for lighting model when producing quads and disable your
     * shading when the lighting model is anything other than vanilla.  The API will force model rebake if the
     * lighting model is changed by the user.<p>
     * 
     * In the enhanced lighting model, this shading only affects <em>block</em> light. Sky light is
     * directional and sky illumination will not be reduced by block shading. (Which is why you shouldn't
     * bake shading into your vertex colors.)  Block light is still shadowed according to Minecraft convention.<p>
     * 
     * Future lighting models that account for light direction in both block and sky light will completely
     * ignore the enableDiffuse setting.<p>
     *  
     * Note that layers with emissive rendering enabled via {@link #setEmissive(int, boolean)} will have
     * both diffuse and AO disabled and this setting will be ignored.
     */
    @Deprecated
    public void setShading(boolean enableDiffuse);
    
    /**
     * TODO: finish - still needed? Should it/can it be by layer?
     * Primarily intended for vanilla model support.  Will be true by default at start of each quad.
     */
    public void setAmbientOcclusion(boolean enableAmbientOcclusion);

}
