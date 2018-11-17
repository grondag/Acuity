package grondag.acuity.hooks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface IBlockInfo
{
    void prepare(IBlockAccess world, IBlockState state, BlockPos pos);

    float[][][] getAoFast();

    int[] getPackedLightFast();

    BlockPos blockPos();
    
    IBlockAccess world();

    float shiftX();
    float shiftY();
    float shiftZ();

    boolean isFullCube();

    float[][][][] getSkyLight();

    float[][][][] getBlockLight();

    int getColorMultiplier(int tint);
}
