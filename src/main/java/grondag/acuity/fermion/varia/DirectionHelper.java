package grondag.acuity.fermion.varia;

import net.minecraft.util.math.Direction;

public class DirectionHelper
{
    private static final Direction[] ALL_DIRECTIONS = Direction.values();
    
    public static Direction fromOrdinal(int ordinal)
    {
        return ALL_DIRECTIONS[ordinal];
    }
}
