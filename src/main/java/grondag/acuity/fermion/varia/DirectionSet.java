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

package grondag.acuity.fermion.varia;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import net.minecraft.util.math.Direction;

import static net.minecraft.util.math.Direction.*;

public class DirectionSet
{
    @SuppressWarnings("unchecked")
    private static Set<Direction>[] ALL_SETS = new Set[64];
    
    public static final Set<Direction> ALL;
    public static final Set<Direction> NONE;
  
    static
    {
        for(int i = 0; i < 64; i++)
        {
            EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
            if((i & (1 << DOWN.ordinal())) != 0)  set.add(DOWN);
            if((i & (1 << UP.ordinal())) != 0)  set.add(UP);
            if((i & (1 << EAST.ordinal())) != 0)  set.add(EAST);
            if((i & (1 << WEST.ordinal())) != 0)  set.add(WEST);
            if((i & (1 << NORTH.ordinal())) != 0)  set.add(NORTH);
            if((i & (1 << SOUTH.ordinal())) != 0)  set.add(SOUTH);
            
            ALL_SETS[i] = Collections.unmodifiableSet(set);
        }
        ALL = ALL_SETS[63];
        NONE = ALL_SETS[0];
    }
    
    public static int addFaceToBit(int bits, Direction face)
    {
        return bits | (1 << face.ordinal());
    }
    
    public static int sharedIndex(Set<Direction> fromSet)
    {
        if(fromSet.isEmpty())
            return 0;
        else if(fromSet.size() == 6)
            return 63;
        else
        {
            int bits = 0;
            if(fromSet.contains(Direction.DOWN)) bits |= (1 << DOWN.ordinal());
            if(fromSet.contains(Direction.UP)) bits |= (1 << UP.ordinal());
            if(fromSet.contains(Direction.EAST)) bits |= (1 << EAST.ordinal());
            if(fromSet.contains(Direction.WEST)) bits |= (1 << WEST.ordinal());
            if(fromSet.contains(Direction.NORTH)) bits |= (1 << NORTH.ordinal());
            if(fromSet.contains(Direction.SOUTH)) bits |= (1 << SOUTH.ordinal());
            
            return bits;
        }    
    }
    
    public static Set<Direction> sharedInstance(Set<Direction> fromSet)
    {
        return sharedInstance(sharedIndex(fromSet));
    }
    
    public static Set<Direction> sharedInstance(int fromIndex)
    {
        return ALL_SETS[fromIndex];
    }
    
}
