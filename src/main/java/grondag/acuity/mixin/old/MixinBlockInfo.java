package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.Acuity;
import grondag.acuity.api.IBlockInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.pipeline.BlockInfo;

@Mixin(BlockInfo.class)
public abstract class MixinBlockInfo implements IBlockInfo
{
    @Shadow(remap=false) private IBlockAccess world;
    @Shadow(remap=false)  private IBlockState state;
    @Shadow(remap=false)  private BlockPos blockPos;
    
    @Shadow(remap=false)  private boolean[][][] t;
    @Shadow(remap=false)  private int[][][] s;
    @Shadow(remap=false)  private int[][][] b;
    @Shadow(remap=false)  private float[][][][] skyLight;
    @Shadow(remap=false)  private float[][][][] blockLight;
    @Shadow(remap=false)  private float[][][] ao;
    @Shadow(remap=false)  private int[] packed;
    @Shadow(remap=false)  private boolean full;
    @Shadow(remap=false)  private float shx;
    @Shadow(remap=false)  private float shy;
    @Shadow(remap=false)  private float shz;
    
    @Shadow(remap=false)  protected abstract float combine(int c, int s1, int s2, int s3, boolean t0, boolean t1, boolean t2, boolean t3);
    
    private boolean needsFlatLightUpdate = true;
    private boolean needsAoLightUpdate = true;
    
    private final MutableBlockPos searchPos = new MutableBlockPos();
    
    /**
     * Consolidate multiple calls to {@link #reset()}, {@link #setBlockPos(net.minecraft.util.math.BlockPos)},
     * {@link #setState(net.minecraft.block.state.IBlockState)} and {@link #setWorld(net.minecraft.world.IBlockAccess)}
     */
    @Override
    public void prepare(IBlockAccess world, IBlockState state, BlockPos pos)
    {
        ((BlockInfo)(Object)this).reset();
        this.needsAoLightUpdate = true;
        this.needsFlatLightUpdate = true;
        this.world = world;
        this.state = state;
        this.blockPos = pos;
        ((BlockInfo)(Object)this).updateShift();
    }
    
    @Override
    public float[][][] getAoFast()
    {
        if(this.needsAoLightUpdate && Acuity.isModEnabled())
        {
            this.needsAoLightUpdate = false;
            updateLightMatrixFast();
        }
        return ao;
    }
    
    @Override
    public int[] getPackedLightFast()
    {
        if(this.needsFlatLightUpdate && Acuity.isModEnabled())
        {
            this.needsFlatLightUpdate = false;
            updateFlatLightingFast();
        }
        return packed;
    }
    private void updateLightMatrixFast()
    {
        for(int x = 0; x <= 2; x++)
        {
            for(int y = 0; y <= 2; y++)
            {
                for(int z = 0; z <= 2; z++)
                {
                    searchPos.setPos(
                            blockPos.getX() + x - 1, 
                            blockPos.getY() + y - 1, 
                            blockPos.getZ() + z - 1);

                    IBlockState state = world.getBlockState(searchPos);
                    t[x][y][z] = state.getLightOpacity(world, searchPos) < 15;
                    int brightness = state.getPackedLightmapCoords(world, searchPos);
                    s[x][y][z] = (brightness >> 0x14) & 0xF;
                    b[x][y][z] = (brightness >> 0x04) & 0xF;
                    ao[x][y][z] = state.getAmbientOcclusionLightValue();
                }
            }
        }
        updateLightMatrixFastInner(EnumFacing.DOWN);
        updateLightMatrixFastInner(EnumFacing.UP);
        updateLightMatrixFastInner(EnumFacing.EAST);
        updateLightMatrixFastInner(EnumFacing.WEST);
        updateLightMatrixFastInner(EnumFacing.NORTH);
        updateLightMatrixFastInner(EnumFacing.SOUTH);

        for(int x = 0; x < 2; x++)
        {
            for(int y = 0; y < 2; y++)
            {
                for(int z = 0; z < 2; z++)
                {
                    int x1 = x * 2;
                    int y1 = y * 2;
                    int z1 = z * 2;

                    int     sxyz = s[x1][y1][z1];
                    int     bxyz = b[x1][y1][z1];
                    boolean txyz = t[x1][y1][z1];

                    int     sxz = s[x1][1][z1], sxy = s[x1][y1][1], syz = s[1][y1][z1];
                    int     bxz = b[x1][1][z1], bxy = b[x1][y1][1], byz = b[1][y1][z1];
                    boolean txz = t[x1][1][z1], txy = t[x1][y1][1], tyz = t[1][y1][z1];

                    int     sx = s[x1][1][1], sy = s[1][y1][1], sz = s[1][1][z1];
                    int     bx = b[x1][1][1], by = b[1][y1][1], bz = b[1][1][z1];
                    boolean tx = t[x1][1][1], ty = t[1][y1][1], tz = t[1][1][z1];

                    skyLight  [0][x][y][z] = combine(sx, sxz, sxy, txz || txy ? sxyz : sx,
                                                     tx, txz, txy, txz || txy ? txyz : tx);
                    blockLight[0][x][y][z] = combine(bx, bxz, bxy, txz || txy ? bxyz : bx,
                                                     tx, txz, txy, txz || txy ? txyz : tx);

                    skyLight  [1][x][y][z] = combine(sy, sxy, syz, txy || tyz ? sxyz : sy,
                                                     ty, txy, tyz, txy || tyz ? txyz : ty);
                    blockLight[1][x][y][z] = combine(by, bxy, byz, txy || tyz ? bxyz : by,
                                                     ty, txy, tyz, txy || tyz ? txyz : ty);

                    skyLight  [2][x][y][z] = combine(sz, syz, sxz, tyz || txz ? sxyz : sz,
                                                     tz, tyz, txz, tyz || txz ? txyz : tz);
                    blockLight[2][x][y][z] = combine(bz, byz, bxz, tyz || txz ? bxyz : bz,
                                                     tz, tyz, txz, tyz || txz ? txyz : tz);
                }
            }
        }
    }
    
    private void updateLightMatrixFastInner(EnumFacing side)
    {
        if(!state.doesSideBlockRendering(world, blockPos, side))
        {
            int x = side.getXOffset() + 1;
            int y = side.getYOffset() + 1;
            int z = side.getZOffset() + 1;
            s[x][y][z] = Math.max(s[1][1][1] - 1, s[x][y][z]);
            b[x][y][z] = Math.max(b[1][1][1] - 1, b[x][y][z]);
        }
    }

    private void updateFlatLightingFast()
    {
        full = state.isFullCube();
        packed[0] = state.getPackedLightmapCoords(world, blockPos);
        updateFlatLightingFastInner(EnumFacing.DOWN);
        updateFlatLightingFastInner(EnumFacing.UP);
        updateFlatLightingFastInner(EnumFacing.EAST);
        updateFlatLightingFastInner(EnumFacing.WEST);
        updateFlatLightingFastInner(EnumFacing.NORTH);
        updateFlatLightingFastInner(EnumFacing.SOUTH);
    }
    
    private void updateFlatLightingFastInner(EnumFacing side)
    {
        searchPos.setPos(blockPos.getX() + side.getXOffset(), blockPos.getY() + side.getYOffset(), blockPos.getZ() + side.getZOffset());
        packed[side.ordinal() + 1] = state.getPackedLightmapCoords(world, searchPos);
    }

    @Override
    public BlockPos blockPos()
    {
        return blockPos;
    }

    @Override
    public IBlockAccess world()
    {
        return world;
    }

    @Override
    public float shiftX()
    {
        return shx;
    }

    @Override
    public float shiftY()
    {
        return shy;
    }

    @Override
    public float shiftZ()
    {
        return shz;
    }
}
