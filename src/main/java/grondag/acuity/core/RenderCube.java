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

package grondag.acuity.core;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class RenderCube
{
    //UGLY: Copypasta from Exotic Matter
    private static final int WORLD_BOUNDARY = 30000000;
    private static final int NUM_X_BITS = 1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(WORLD_BOUNDARY));
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 8;
    
    private static final int Y_SHIFT = 0 + NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;
    
    /**
     * Finds the origin of the 256x256x256 render cube for the given coordinate.
     * Works for X, Y, and Z.
     */
    public static final int renderCubeOrigin(int worldCoord)
    {
        return worldCoord & 0xFFFFFF00;
    }
    
    /**
     * Returns coordinate value relative to its origin. Essentially a macro for
     * worldCood - {@link #renderCubeOrigin(int)}
     */
    public static final int renderCubeRelative(int worldCoord)
    {
        return worldCoord - renderCubeOrigin(worldCoord);
    }
    
    /**
     * Floating point version - retains fractional component.
     */
    public static final float renderCubeRelative(float worldCoord)
    {
        return worldCoord - renderCubeOrigin(MathHelper.floor(worldCoord));
    }

    /**
     * Packs cube position corresponding with the given position into a single long value.
     * For now, assume Y coordinates are limited to 0-255.
     */
    public static long getPackedOrigin(BlockPos position)
    {
        return pack(renderCubeOrigin(position.getX()), renderCubeOrigin(position.getY()), renderCubeOrigin(position.getZ()));
    }
    
    public static int getPackedKeyOriginX(long packedKey)
    {
        return (int)((packedKey >> X_SHIFT) & X_MASK) - WORLD_BOUNDARY;
    }
    
    public static int getPackedKeyOriginZ(long packedKey)
    {
        return (int)(packedKey & Z_MASK) - WORLD_BOUNDARY;
    }
    
    public static int getPackedKeyOriginY(long packedKey)
    {
        return (int)((packedKey >> Y_SHIFT) & Y_MASK);
    }
    
    private static final long pack(int x, int y, int z)
    {
        return ((long)(x + WORLD_BOUNDARY) & X_MASK) << X_SHIFT | ((long)y & Y_MASK) << Y_SHIFT | ((long)(z + WORLD_BOUNDARY) & Z_MASK);
    }
}
