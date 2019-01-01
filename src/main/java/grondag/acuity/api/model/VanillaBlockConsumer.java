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

import static net.minecraft.util.math.MathHelper.equalsApproximate;

import grondag.acuity.buffer.VertexCollector;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.ExtendedBlockView;

public class VanillaBlockConsumer extends BlockVertexConsumerImpl implements BlockVertexConsumer
{
    private final BlockPos.Mutable lightPos = new BlockPos.Mutable();
    private boolean isModelBlockIsOccluderDoNotUseDirectly = false;
    private boolean isOcclusionLookupDirty = true;
    
    private boolean isModelBlockOccluder()
    {
        if(isOcclusionLookupDirty)
            isModelBlockIsOccluderDoNotUseDirectly = blockState.method_11604(world, pos);
        return isModelBlockIsOccluderDoNotUseDirectly;
    }
    
    @Override
    public final BlockVertexConsumer prepare(BlockPos pos, ExtendedBlockView world, BlockState state, BlockEntity blockEntity)
    {
        super.prepareInner(pos, world, state, blockEntity);
        isOcclusionLookupDirty = true;
        return this;
    }

    @Override
    public final boolean isStandardLightingModel()
    {
        return true;
    }

    @Override
    protected final void lightAndOutputQuad()
    {
        clasifyShape();
        
        // check for face-planar quad - skip if not visible
        if(enableOcclusionFilter && isOnBlockFace && !shouldDrawSide(actualDirection))
            return;
        
        final int encodedShiftedBlendFlags = (mipMapFlags | (cutoutFlags << 3)) << 24;
        final int encodedShiftedLightFlags = (lightFlags & VERTEX_LIGHT_ENABLE_MASK) << 24;
        final boolean useNeighborBrightness = this.isOnBlockFace || this.isModelBlockOccluder();
        
        int blockLight = 0;
        int skyLight = 0;
        final boolean smoothLight = (lightFlags & AO_MASK) != 0;
        
        if(smoothLight)
            aoCalc.compute(world, blockState, lightPos, actualDirection, aoBounds, useNeighborBrightness, !isAoCubic);
        else
        {
            lightPos.set(pos);
            if(useNeighborBrightness)
                lightPos.offset(actualDirection);
            
            // what we get back is raw (0-15) sky << 20 | block << 4
            // we want to output 0-255
            final int packedLight =  aoCalc.cachedBrightness(blockState, world, lightPos);
            blockLight = ((packedLight >> 4) & 0xF) * 17;
            skyLight = ((packedLight >> 20) & 0xF) * 17;
        }
        
        VertexCollector output = getVertexCollector();
        
        int v = 0; // for AO lookup
        
        for(int i = 0; i < QUAD_STRIDE; i += VERTEX_STRIDE)
        {
            // convert color to luminance
            int vertexLight = vertexData[i + LIGHTMAP];
            if(vertexLight != 0)
                vertexLight = Math.round(
                            (vertexLight & 0xFF) * 0.2126f
                          + ((vertexLight >> 8) & 0xFF) * 0.7152f
                          + ((vertexLight >> 16) & 0xFF) * 0.0722f);
            
            // compute shifted vertex positions
            final float x = Float.intBitsToFloat(vertexData[i + POS_X]) + offsetX;
            final float y = Float.intBitsToFloat(vertexData[i + POS_Y]) + offsetY; 
            final float z = Float.intBitsToFloat(vertexData[i + POS_Z]) + offsetZ;
            
            // retrieve vertex normals for lighting
            float normX, normY, normZ;
            normX = Float.intBitsToFloat(vertexData[i + NORM_X]);
            if(Float.isNaN(normX))
            {
                normX = faceNormX;
                normY = faceNormY;
                normZ = faceNormZ;
            }
            else
            {
                normY = Float.intBitsToFloat(vertexData[i + NORM_Y]);
                normZ = Float.intBitsToFloat(vertexData[i + NORM_Z]);
            }
            
            // Formula below mimics vanilla diffuse lighting for plane-aligned quads
            // and gives a cubic-weighted average in other cases. (sum of normal component squares == 1.0)
            // The (3f + normY) / 4f component gives 0.5 for down face and 1.0 for up face.
            // Min is to handle rounding errors.
            // Can skip this if no surface wants diffiuse shading
            final float baseShade = (lightFlags & DIFFUSE_MASK) == 0
                    ? 1 : Math.min(normX * normX * 0.6f + normY * normY * ((3f + normY) / 4f) + normZ * normZ * 0.8f, 1f);
            
            
            float worldShade = (lightFlags & WORLD_DIFFUSE_FLAG) == 0 ? 1.0f : baseShade;
            float vertexShade = (lightFlags & VERTEX_DIFFUSE_FLAG) == 0 ? 1.0f : baseShade;
            
            // FIX??  Slight inconsistency here in that we use smooth light for all
            // vertices if any vertex requires AO.  Shouldn't matter for vanilla blocks which are all single layer.
            if(smoothLight)
            {
                final int packedLight =  aoCalc.brightness[v];
                blockLight = ((packedLight >> 4) & 0xF) * 17;
                skyLight = ((packedLight >> 20) & 0xF) * 17;
            }
            
            if((lightFlags & WORLD_AO_FLAG) != 0)
                worldShade *= aoCalc.colorMultiplier[v++];
            
            if((lightFlags & VERTEX_AO_FLAG) != 0)
                vertexShade *= aoCalc.colorMultiplier[v++];
            
            output.pos(pos, x, y, z);
        
            output.add(ColorHelper.shadeColorAndSwapRedBlue(vertexData[i + COLOR_0], 
                    (lightFlags & 1) == 1 ? vertexShade : worldShade));
            
            output.add(Float.intBitsToFloat(vertexData[i + U_0]));
            output.add(Float.intBitsToFloat(vertexData[i + V_0]));
            
            int normBlend = Math.round(normX * 127 + 127);
            normBlend |= (Math.round(normY * 127 + 127) << 8);
            normBlend |= (Math.round(normZ * 127 + 127) << 16);
            output.add(encodedShiftedBlendFlags | normBlend);
            
            output.add(blockLight | (skyLight << 8) | (vertexLight << 16) | encodedShiftedLightFlags);
            
            if(textureDepth != TextureDepth.SINGLE)
            {
                output.add(ColorHelper.shadeColorAndSwapRedBlue(vertexData[i + COLOR_1], 
                        (lightFlags & 2) == 2 ? vertexShade : worldShade));
                output.add(Float.intBitsToFloat(vertexData[i + U_1]));
                output.add(Float.intBitsToFloat(vertexData[i + V_1]));
                
                if(textureDepth == TextureDepth.TRIPLE)
                {
                    output.add(ColorHelper.shadeColorAndSwapRedBlue(vertexData[i + COLOR_2], 
                            (lightFlags & 4) == 4 ? vertexShade : worldShade));
                    output.add(Float.intBitsToFloat(vertexData[i + U_2]));
                    output.add(Float.intBitsToFloat(vertexData[i + V_2]));
                }
            }
        }
    }

    private VertexCollector getVertexCollector()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    final AoCalculator aoCalc = new AoCalculator();
    final float[] aoBounds = new float[12];

    static final float EPSILON_MIN = 1.0E-4F;
    static final float EPSILON_MAX = 0.9999F;
    
    /**
     * Direction implied by vertex winding order.
     */
    Direction actualDirection = null;
    
    /**
     * True if quad is co-planar with the cube face associated with actual direction.
     */
    boolean isOnBlockFace;
    
    /**
     * True if quad vertices touch all four corners orthogonal to actual direction.<br>
     * Used by AO to know if interpolated light calculations are necessary.
     */
    boolean isAoCubic;
    
    /**
     * Adapted from BlockModelRenderer
     */
    private void clasifyShape()
    {
        float minX = Float.intBitsToFloat(vertexData[POS_X]);
        float minY = Float.intBitsToFloat(vertexData[POS_Y]);
        float minZ = Float.intBitsToFloat(vertexData[POS_Z]);
        float maxX = minX;
        float maxY = minY;
        float maxZ = minZ;

        for(int offset = VERTEX_STRIDE; offset < QUAD_STRIDE; offset += VERTEX_STRIDE)
        {
           final float x = Float.intBitsToFloat(vertexData[offset + POS_X]);
           if(x < minX)
               minX = x;
           else if(x > maxX)
               maxX = x;
           
           final float y = Float.intBitsToFloat(vertexData[offset + POS_Y]);
           if(y < minY)
               minY = y;
           else if(y > maxY)
               maxY = y;
           
           final float z = Float.intBitsToFloat(vertexData[offset + POS_Z]);
           if(z < minZ)
               minZ = z;
           else if(z > maxZ)
               maxZ = z;
        }

        if((lightFlags & AO_MASK) != 0)
        {
            aoBounds[Direction.WEST.getId()] = minX;
            aoBounds[Direction.EAST.getId()] = maxX;
            aoBounds[Direction.DOWN.getId()] = minY;
            aoBounds[Direction.UP.getId()] = maxY;
            aoBounds[Direction.NORTH.getId()] = minZ;
            aoBounds[Direction.SOUTH.getId()] = maxZ;
            aoBounds[Direction.WEST.getId() + 6] = 1.0F - minX;
            aoBounds[Direction.EAST.getId() + 6] = 1.0F - maxX;
            aoBounds[Direction.DOWN.getId() + 6] = 1.0F - minY;
            aoBounds[Direction.UP.getId() + 6] = 1.0F - maxY;
            aoBounds[Direction.NORTH.getId() + 6] = 1.0F - minZ;
            aoBounds[Direction.SOUTH.getId() + 6] = 1.0F - maxZ;
        }
        
        switch(longestAxis())
        {
            case X:
            {
                boolean onPlane = equalsApproximate(minX, maxX);
                if(faceNormX > 0)
                {
                    actualDirection = Direction.EAST;
                    isOnBlockFace = onPlane && maxX >= EPSILON_MAX;
                }
                else
                {
                    actualDirection = Direction.WEST;
                    isOnBlockFace = onPlane && minX <= EPSILON_MIN;
                }
                isAoCubic = isOnBlockFace && minY <= EPSILON_MIN && minZ <= EPSILON_MIN && maxY >= EPSILON_MAX && maxZ >= EPSILON_MAX;
                break;
            }
            case Y:
            {
                boolean onPlane = equalsApproximate(minY, maxY);
                if(faceNormY > 0)
                {
                    actualDirection = Direction.UP;
                    isOnBlockFace = onPlane && maxY >= EPSILON_MAX;
                }
                else
                {
                    actualDirection = Direction.DOWN;
                    isOnBlockFace = onPlane && minY <= EPSILON_MIN;
                }
                isAoCubic = isOnBlockFace && minX <= EPSILON_MIN && minZ <= EPSILON_MIN && maxX >= EPSILON_MAX && maxZ >= EPSILON_MAX;
                break;
            }
            case Z:
            {
                boolean onPlane = equalsApproximate(minZ, maxZ);
                if(faceNormZ > 0)
                {
                    actualDirection = Direction.SOUTH;
                    isOnBlockFace = onPlane && maxZ >= EPSILON_MAX;
                }
                else
                {
                    actualDirection = Direction.NORTH;
                    isOnBlockFace = onPlane && minZ <= EPSILON_MIN;
                }
                isAoCubic = isOnBlockFace && minX <= EPSILON_MIN && minY <= EPSILON_MIN && maxX >= EPSILON_MAX && maxY >= EPSILON_MAX;
                break;
                
            }
        }
     }
    
    private Axis longestAxis()
    {
        Axis result = Axis.Y;
        float longest = Math.abs(faceNormY);
            
        float a = Math.abs(faceNormX);
        if(a > longest)
        {
            result = Axis.X;
            longest = a;
        }
        
        return Math.abs(faceNormZ) > longest
                ? Axis.Z : result;
    }
}

   
