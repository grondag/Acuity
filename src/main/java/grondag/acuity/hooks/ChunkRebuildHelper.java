package grondag.acuity.hooks;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

public class ChunkRebuildHelper
{
    private static final ThreadLocal<ChunkRebuildHelper> HELPERS = new ThreadLocal<ChunkRebuildHelper>()
    {
        @Override
        protected ChunkRebuildHelper initialValue()
        {
            return new ChunkRebuildHelper();
        }
    };
    
    public static ChunkRebuildHelper get()
    {
        return HELPERS.get();
    }
    
    public final BlockRenderLayer[] layers = BlockRenderLayer.values().clone();
    public final int layerCount = layers.length;
    private final boolean[] emptyFlagsDoNotUse = new boolean[layerCount];
    public final boolean[] layerFlags = new boolean[layerCount];
    public final MutableBlockPos searchPos = new MutableBlockPos();
    public final HashSet<TileEntity> tileEntities = Sets.newHashSet();
    public final Set<TileEntity> tileEntitiesToAdd = Sets.newHashSet();
    public final Set<TileEntity> tileEntitiesToRemove = Sets.newHashSet();
    private final BufferBuilder[] builders = new BufferBuilder[layerCount];
    
    public BufferBuilder[] builders(RegionRenderCacheBuilder regionCache)
    {
        builders[BlockRenderLayer.SOLID.ordinal()] = regionCache.getWorldRendererByLayer(BlockRenderLayer.SOLID);
        builders[BlockRenderLayer.CUTOUT.ordinal()] = regionCache.getWorldRendererByLayer(BlockRenderLayer.CUTOUT);
        builders[BlockRenderLayer.CUTOUT_MIPPED.ordinal()] = regionCache.getWorldRendererByLayer(BlockRenderLayer.CUTOUT_MIPPED);
        builders[BlockRenderLayer.TRANSLUCENT.ordinal()] = regionCache.getWorldRendererByLayer(BlockRenderLayer.TRANSLUCENT);
        return builders;
    }

    public void clear()
    {
        System.arraycopy(emptyFlagsDoNotUse, 0, layerFlags, 0, layerCount);
        tileEntities.clear();
        tileEntitiesToAdd.clear();
        tileEntitiesToRemove.clear();
    }
}