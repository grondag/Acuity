package grondag.acuity.core;

import net.minecraft.util.math.MathHelper;

public class Utility
{
    /**
     * Finds the origin of the 256x256x256 render cube for the given coordinate.
     * Works for X, Y, and Z, but note that Y coords (0-255) will always result in 0.
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
}
