package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;

@Mixin(BiomeColorHelper.class)
public abstract class MixinBiomeColorHelper
{
    //TODO: make this configurable/optional
    
    @Redirect(method = "getGrassColorAtPos", expect = 1,       
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/BiomeColorHelper;getColorAtPos(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/biome/BiomeColorHelper$ColorResolver;)I"))       
    private static int onGetGrassColorAtPos(IBlockAccess blockAccess, BlockPos pos, BiomeColorHelper.ColorResolver colorResolver)
    {
        return getColorAtPosFast(blockAccess, pos, colorResolver);
    }

    @Redirect(method = "getFoliageColorAtPos", expect = 1,       
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/BiomeColorHelper;getColorAtPos(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/biome/BiomeColorHelper$ColorResolver;)I"))       
    private static int onGetFoliageColorAtPos(IBlockAccess blockAccess, BlockPos pos, BiomeColorHelper.ColorResolver colorResolver)
    {
        return getColorAtPosFast(blockAccess, pos, colorResolver);
    }

    @Redirect(method = "getWaterColorAtPos", expect = 1,       
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/BiomeColorHelper;getColorAtPos(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/biome/BiomeColorHelper$ColorResolver;)I"))       
    private static int onGetWaterColorAtPos(IBlockAccess blockAccess, BlockPos pos, BiomeColorHelper.ColorResolver colorResolver)
    {
        return getColorAtPosFast(blockAccess, pos, colorResolver);
    }
    
    private static final ThreadLocal<BlockPos.Mutable> searchPos = new ThreadLocal<BlockPos.Mutable>()
    {
        @Override
        protected BlockPos.Mutable initialValue()
        {
            return new BlockPos.Mutable();
        }
    };

    private static int getColorAtPosFast(IBlockAccess blockAccess, BlockPos pos, BiomeColorHelper.ColorResolver colorResolver)
    {
        int i = 0;
        int j = 0;
        int k = 0;
        BlockPos.Mutable mPos = searchPos.get();
        
        for(int x = -1; x <= 1; x++)
        {
            for(int z = -1; z <= 1; z++)
            {
                mPos.set(pos.getX() + x, pos.getY(), pos.getZ() + z);
                int l = colorResolver.getColorAtPos(blockAccess.getBiome(mPos), mPos);
                i += (l & 16711680) >> 16;
                j += (l & 65280) >> 8;
                k += l & 255;
            }
        }

        return (i / 9 & 255) << 16 | (j / 9 & 255) << 8 | k / 9 & 255;
    }
}
