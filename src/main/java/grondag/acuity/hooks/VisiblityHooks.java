package grondag.acuity.hooks;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class VisiblityHooks
{
    @SuppressWarnings("unchecked")
    public static Set<EnumFacing> getVisibleFacingsExt(Object visData, BlockPos eyePos)
    {
        Set<EnumFacing> result;
        
        if(visData instanceof Set)
            result = (Set<EnumFacing>)visData;
        else
        {
            Int2ObjectOpenHashMap<Set<EnumFacing>> facingMap = (Int2ObjectOpenHashMap<Set<EnumFacing>>)visData;
            result = facingMap.get(VisGraph.getIndex(eyePos));
            if(result == null)
                result = EnumFacingSet.NONE;
        }
        
        return result;
    }
    
    
    public static SetVisibility computeVisiblityExt(VisGraph visgraph)
    {
        SetVisibility setvisibility = new SetVisibility();

        if (4096 - visgraph.empty < 256)
        {
            setvisibility.setAllVisible(true);
            ((ISetVisibility)setvisibility).setVisibilityData(EnumFacingSet.ALL);
        }
        else if (visgraph.empty == 0)
        {
            setvisibility.setAllVisible(false);
            ((ISetVisibility)setvisibility).setVisibilityData(EnumFacingSet.NONE);
        }
        else
        {
            final BitSet bitSet = visgraph.bitSet;
            Int2ObjectOpenHashMap<Set<EnumFacing>> facingMap = new Int2ObjectOpenHashMap<Set<EnumFacing>>();
            
            for (int i : VisGraph.INDEX_OF_EDGES)
            {
                if (!bitSet.get(i))
                {
                    final Pair<Set<EnumFacing>, IntArrayList> floodResult = floodFill(visgraph, i);
                    final Set<EnumFacing> fillSet = floodResult.getLeft();
                    setvisibility.setManyVisible(fillSet);
                    IntListIterator it = floodResult.getRight().iterator();
                    while(it.hasNext())
                        facingMap.put(it.nextInt(), fillSet);
                }
            }
            ((ISetVisibility)setvisibility).setVisibilityData(facingMap);
        }

        return setvisibility;
    }

    private static class Helpers
    {
        final EnumSet<EnumFacing> faces = EnumSet.noneOf(EnumFacing.class);
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
    
    private static Pair<Set<EnumFacing>, IntArrayList> floodFill(VisGraph visgraph, int pos)
    {
        final Helpers help = helpers.get();
        Set<EnumFacing> set = help.faces;
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

            for (EnumFacing enumfacing : EnumFacing.values())
            {
                int j = visgraph.getNeighborIndexAtFace(i, enumfacing);

                if (j >= 0 && !visgraph.bitSet.get(j))
                {
                    visgraph.bitSet.set(j, true);
                    queue.enqueue(j);
                    list.add(j);
                }
            }
        }

        return Pair.of(EnumFacingSet.sharedInstance(set), list);
    }
}
