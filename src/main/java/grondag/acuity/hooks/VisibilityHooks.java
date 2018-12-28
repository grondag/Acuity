package grondag.acuity.hooks;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class VisibilityHooks
{
    private static final Direction[] ALL_DIRECTIONS = Direction.values();
    
    @SuppressWarnings("unchecked")
    public static Set<Direction> getVisibleFacingsExt(Object visData, BlockPos eyePos)
    {
        if(visData instanceof Set)
            return (Set<Direction>)visData;
        else
        {
            return ((VisibilityMap)visData).getFaceSet(VisGraph.getIndex(eyePos));
        }
    }
    
    public static SetVisibility computeVisiblityExt(VisGraph visgraph)
    {
        SetVisibility setvisibility = new SetVisibility();

        if (4096 - visgraph.empty < 256)
        {
            setvisibility.setAllVisible(true);
            ((ISetVisibility)setvisibility).setVisibilityData(DirectionSet.ALL);
        }
        else if (visgraph.empty == 0)
        {
            setvisibility.setAllVisible(false);
            ((ISetVisibility)setvisibility).setVisibilityData(DirectionSet.NONE);
        }
        else
        {
            final BitSet bitSet = visgraph.bitSet;
            VisibilityMap facingMap = VisibilityMap.claim();
            
            for (int i : VisGraph.INDEX_OF_EDGES)
            {
                if (!bitSet.get(i))
                {
                    final Pair<Set<Direction>, IntArrayList> floodResult = floodFill(visgraph, i);
                    final Set<Direction> fillSet = floodResult.getLeft();
                    setvisibility.setManyVisible(fillSet);
                    byte setIndex = (byte) DirectionSet.sharedIndex(fillSet);
                    final IntArrayList list = floodResult.getRight();
                    final int limit = list.size();
                    for(int j = 0; j < limit; j++)
                        facingMap.setIndex(list.getInt(j), setIndex);
                }
            }
            ((ISetVisibility)setvisibility).setVisibilityData(facingMap);
        }

        return setvisibility;
    }

    private static class Helpers
    {
        final EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        final IntArrayList list = new IntArrayList();
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    }
    
    private static final ThreadLocal<Helpers> helpers = new ThreadLocal<Helpers>()
    {
        @Override
        protected Helpers initialValue()
        {
            return new Helpers();
        }
    };
    
    private static Pair<Set<Direction>, IntArrayList> floodFill(VisGraph visgraph, int pos)
    {
        final Helpers help = helpers.get();
        Set<Direction> set = help.faces;
        set.clear();
        
        final IntArrayList list = help.list;
        list.clear();
        
        final IntArrayFIFOQueue queue = help.queue;
        queue.clear();
        
        queue.enqueue(pos);
        list.add(pos);
        
        visgraph.bitSet.set(pos, true);

        while (!queue.isEmpty())
        {
            int i = queue.dequeueInt();
            visgraph.addEdges(i, set);

            for(int f = 0; f < 6; f++)
            {
                final Direction enumfacing = ALL_DIRECTIONS[f];
                
                int j = visgraph.getNeighborIndexAtFace(i, enumfacing);

                if (j >= 0 && !visgraph.bitSet.get(j))
                {
                    visgraph.bitSet.set(j, true);
                    queue.enqueue(j);
                    list.add(j);
                }
            }
        }

        return Pair.of(DirectionSet.sharedInstance(set), list);
    }
}
