package grondag.acuity.hooks;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import grondag.acuity.core.SetVisibilityExt;
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
    public static Set<EnumFacing> getVisibleFacingsExt(SetVisibilityExt rawVis, BlockPos eyePos)
    {
        final Object facings = ((SetVisibilityExt)rawVis).visibility;
        EnumSet<EnumFacing> result;
        
        if(facings instanceof EnumSet)
            result = (EnumSet<EnumFacing>)facings;
        else
        {
            Int2ObjectOpenHashMap<EnumSet<EnumFacing>> facingMap = (Int2ObjectOpenHashMap<EnumSet<EnumFacing>>)facings;
            result = facingMap.get(VisGraph.getIndex(eyePos));
            if(result == null)
                result = EnumSet.<EnumFacing>noneOf(EnumFacing.class);
        }
        
        return result;
    }
    
    public static SetVisibility computeVisiblityExt(VisGraph visgraph)
    {
        SetVisibilityExt setvisibility = new SetVisibilityExt();

        if (4096 - visgraph.empty < 256)
        {
            setvisibility.setAllVisible(true);
            setvisibility.visibility = EnumSet.<EnumFacing>allOf(EnumFacing.class);
        }
        else if (visgraph.empty == 0)
        {
            setvisibility.setAllVisible(false);
            setvisibility.visibility = EnumSet.<EnumFacing>noneOf(EnumFacing.class);
        }
        else
        {
            final BitSet bitSet = visgraph.bitSet;
            Int2ObjectOpenHashMap<EnumSet<EnumFacing>> facingMap = new Int2ObjectOpenHashMap<EnumSet<EnumFacing>>();
            
            for (int i : VisGraph.INDEX_OF_EDGES)
            {
                if (!bitSet.get(i))
                {
                    final Pair<EnumSet<EnumFacing>, IntArrayList> floodResult = floodFill(visgraph, i);
                    final EnumSet<EnumFacing> fillSet = floodResult.getLeft();
                    setvisibility.setManyVisible(fillSet);
                    IntListIterator it = floodResult.getRight().iterator();
                    while(it.hasNext())
                        facingMap.put(it.nextInt(), fillSet);
                }
            }
            setvisibility.visibility = facingMap;
        }

        return setvisibility;
    }

    private static Pair<EnumSet<EnumFacing>, IntArrayList> floodFill(VisGraph visgraph, int pos)
    {
        EnumSet<EnumFacing> set = EnumSet.<EnumFacing>noneOf(EnumFacing.class);
        IntArrayList list = new IntArrayList();
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
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

        return Pair.of(set, list);
    }
}
