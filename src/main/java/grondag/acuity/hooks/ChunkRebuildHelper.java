package grondag.acuity.hooks;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.BlockLayeredBufferBuilder;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class ChunkRebuildHelper
{
    public static final int BLOCK_RENDER_LAYER_COUNT = BlockRenderLayer.values().length;
    public static final boolean[] EMPTY_RENDER_LAYER_FLAGS = new boolean[BLOCK_RENDER_LAYER_COUNT];
    
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
    public final boolean[] layerFlags = new boolean[BLOCK_RENDER_LAYER_COUNT];
    public final BlockPos.Mutable searchPos = new BlockPos.Mutable();
    public final HashSet<BlockEntity> tileEntities = Sets.newHashSet();
    public final Set<BlockEntity> tileEntitiesToAdd = Sets.newHashSet();
    public final Set<BlockEntity> tileEntitiesToRemove = Sets.newHashSet();
    private final BufferBuilder[] builders = new BufferBuilder[BLOCK_RENDER_LAYER_COUNT];
    
    public BufferBuilder[] builders(BlockLayeredBufferBuilder regionCache)
    {
        builders[BlockRenderLayer.SOLID.ordinal()] = regionCache.get(BlockRenderLayer.SOLID);
        builders[BlockRenderLayer.CUTOUT.ordinal()] = regionCache.get(BlockRenderLayer.CUTOUT);
        builders[BlockRenderLayer.MIPPED_CUTOUT.ordinal()] = regionCache.get(BlockRenderLayer.MIPPED_CUTOUT);
        builders[BlockRenderLayer.TRANSLUCENT.ordinal()] = regionCache.get(BlockRenderLayer.TRANSLUCENT);
        return builders;
    }

    public void clear()
    {
        System.arraycopy(EMPTY_RENDER_LAYER_FLAGS, 0, layerFlags, 0, BLOCK_RENDER_LAYER_COUNT);
        tileEntities.clear();
        tileEntitiesToAdd.clear();
        tileEntitiesToRemove.clear();
    }
}