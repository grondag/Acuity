package grondag.acuity.core;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import grondag.acuity.Acuity;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class VisiblityHooks
{
    /**
     * Called from {@link RenderGlobal#setupTerrain(net.minecraft.entity.Entity, double, net.minecraft.client.renderer.culling.ICamera, int, boolean)}.
     * Relies on pre-computed visibility stored during render chunk rebuild vs computing on fly each time.
     */
    public static Set<EnumFacing> getVisibleFacings(RenderChunk renderchunk, BlockPos eyePos)
    {
        return Acuity.isModEnabled() ? getVisibleFacingsExt(renderchunk, eyePos) : Minecraft.getMinecraft().renderGlobal.getVisibleFacings(eyePos);
    }
    
    @SuppressWarnings("unchecked")
    private static Set<EnumFacing> getVisibleFacingsExt(RenderChunk renderchunk, BlockPos eyePos)
    {
        // unbuilt chunks won't have extended info
        SetVisibility rawVis = renderchunk.compiledChunk.setVisibility;
        if(rawVis instanceof SetVisibilityExt)
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
        else
            return EnumSet.<EnumFacing>noneOf(EnumFacing.class);
    }

    /**
     * Called from {@link RenderChunk#rebuildChunk(float, float, float, net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator)} to
     * store pre-computed visibility for use during render as a performance optimization.
     */
    public static SetVisibility computeVisiblity(VisGraph visgraph)
    {
        return Acuity.isModEnabled() ? computeVisiblityExt(visgraph) : visgraph.computeVisibility();
    }
    
    private static SetVisibility computeVisiblityExt(VisGraph visgraph)
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
