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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

/**
 * Block-specific subtype of {@link VertexConsumer}. (Read docs for parent interface first.)<p>
 * 
 * Block models that use position-based color are responsible for retrieving and applying block 
 * tint to color before they sent to consumer.  The is because the consumer has no way to k
 * now which colors should be modified when there is more than one color/texture layer.<p>
 * 
 */
@Environment(EnvType.CLIENT)
public interface BlockVertexConsumer extends VertexConsumer
{

    
    /**
     * By default, when rendering blocks in the world, the consumer will determine
     * if incoming quads are coplanar with a block face and ignore the quad if that face isn't visible.<p>
     * 
     * If you are checking {@link #shouldDrawSide(Direction)} or if your model contains only
     * quads that are not coplanar, you can improve performance slightly by turning this feature off.<p>
     * 
     * Will be true when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent quads/vertices until changed.<p>
     */
    public void setAutomaticCullingEnabled(boolean isEnabled);
    
    
    /**
     * If true, the next completed quad will be rendered in the translucent render pass.
     * If false, the quad will render in the solid render pass.
     * False by default.<p>
     * 
     * Enabling translucent rendering will clear any pipeline you have set because
     * translucent render is only available with the default pipelines.<p>
     * 
     * Translucent rendering comes with a significant performance penalty and should
     * only be used for glass, force fields or similar surfaces that require texture
     * blending with the scene background.<p>
     * 
     * When enabled, rendered opacity will be texture alpha multiplied by (interpolated) vertex color alpha.<br>
     * For at least some portion of your quad, both of those values should be greater than zero 
     * (or nothing will render) and at least one value should be less than 1.0. 
     * Otherwise there is no point to a translucent render.<p>
     * 
     * The same guidance applies to multi-texture quads.  Texture layers will be blended with each
     * other first and then blended with the scene background.  Secondary and tertiary layers will
     * be blended on "top" of lower texture layers. <p>
     * 
     * Note that you can do cutout renders in <em>both</em> solid and translucent passes and you 
     * do not need to enable translucency for cutout textures.  See {@link #enableCutout(TextureDepth, boolean)}. 
     *
     * Will be false when {@link BlockVertexProvider#produceQuads(AcuityVertexConsumer)} is called.<br>
     * Changes apply to all subsequent quads until changed.<p>
     */
    public void setTranslucent(boolean isTranslucent);
}
