package grondag.acuity.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

@Environment(EnvType.CLIENT)
public interface IBlockInfo
{
    void prepare(ExtendedBlockView world, BlockState state, BlockPos pos);

    float[][][] getAoFast();

    int[] getPackedLightFast();

    BlockPos blockPos();
    
    ExtendedBlockView world();

    float shiftX();
    float shiftY();
    float shiftZ();

    //FIXME: MCP name?
    boolean isFullCube();

    //FIXME: MCP name?
    float[][][][] getSkyLight();

    //FIXME: MCP name?
    float[][][][] getBlockLight();

    //FIXME: MCP name?
    int getColorMultiplier(int tint);
}
