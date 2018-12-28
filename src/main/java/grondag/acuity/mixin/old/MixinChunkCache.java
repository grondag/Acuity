package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.fermion.varia.DirectionHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

@Mixin(ChunkCache.class)
public abstract class MixinChunkCache
{
    @Shadow protected int chunkX;
    @Shadow protected int chunkZ;
    @Shadow protected Chunk[][] chunkArray;
    @Shadow protected World world;
    @Shadow public abstract IBlockState getBlockState(BlockPos pos);
    @Shadow public abstract int getLightFor(EnumSkyBlock type, BlockPos pos);
    @Shadow(remap=false) protected abstract boolean withinBounds(int x, int z);
    
    private static final ThreadLocal<BlockPos.Mutable> fastPos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };
    
    // Low-garbage implementation, matters during high chunk rebuild volume
    @Redirect(method = "getCombinedLight", require = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ChunkCache;getLightForExt(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)I"))
    private int onGetLightForExt(ChunkCache chunkCache, EnumSkyBlock type, BlockPos pos)
    {
        // TODO: make optional?
        
        if (type == EnumSkyBlock.SKY && !this.world.provider.hasSkyLight())
        {
            return 0;
        }
        else if (pos.getY() >= 0 && pos.getY() < 256)
        {
            if (this.getBlockState(pos).useNeighborBrightness())
            {
                int l = 0;

                final BlockPos.Mutable searchPos = fastPos.get();
                
                for(int i = 0; i < 6; i++)
                {
                    Direction face = DirectionHelper.fromOrdinal(i);
                    searchPos.set(pos.getX() + face.getOffsetX(), pos.getY() + face.getOffsetY(), pos.getZ() + face.getOffsetZ());
                    
                    int k = this.getLightFor(type, searchPos);

                    if(k >= 15)
                        return k;
                    else if(k > l)
                        l = k;
                }

                return l;
            }
            else
            {
                int i = (pos.getX() >> 4) - this.chunkX;
                int j = (pos.getZ() >> 4) - this.chunkZ;
                if (!withinBounds(i, j)) return type.defaultLightValue;
                return this.chunkArray[i][j].getLightFor(type, pos);
            }
        }
        else
        {
            return type.defaultLightValue;
        }
    }
}
