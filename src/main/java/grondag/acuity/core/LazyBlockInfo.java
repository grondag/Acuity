//package grondag.acuity.core;
//
//import java.lang.reflect.Field;
//
//import grondag.acuity.Acuity;
//import net.minecraft.block.state.IBlockState;
//import net.minecraft.client.renderer.color.BlockColors;
//import net.minecraft.util.EnumFacing;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.text.translation.I18n;
//import net.minecraft.world.IBlockAccess;
//import net.minecraft.world.World;
//import net.minecraftforge.client.model.pipeline.BlockInfo;
//import net.minecraftforge.fml.relauncher.Side;
//import net.minecraftforge.fml.relauncher.SideOnly;
//
//@SuppressWarnings({ "null"})
//@SideOnly(Side.CLIENT)
//public class LazyBlockInfo extends BlockInfo
//{
//    // forge made parent fields private for whatever reasons...
//    private static final Field fdWorld;
//    private static final Field fdState;
//    private static final Field fdPos;
//    
//    private static boolean fastPrepare;
//    
//    static
//    {
//        boolean isFast = false;
//        Field world = null;
//        Field state = null;
//        Field pos = null;
//        try
//        {
//            world = BlockInfo.class.getDeclaredField("world");
//            world.setAccessible(true);
//            state = BlockInfo.class.getDeclaredField("state");
//            state.setAccessible(true);
//            pos = BlockInfo.class.getDeclaredField("blockPos");
//            pos.setAccessible(true);
//            
//            // confirm it works
//            LazyBlockInfo testTarget = new LazyBlockInfo(null);
//            world.set(testTarget, null);
//            state.set(testTarget, null);
//            pos.set(testTarget, null);
//            
//            isFast = true;
//        }
//        catch(Exception e)
//        {
//            Acuity.INSTANCE.getLog().info(I18n.translateToLocal("misc.warn_slow_block_info"), e);
//        }
//        fastPrepare = isFast;
//        fdWorld = world;
//        fdPos  = pos;
//        fdState = state;
//        
//    }
//    
//    private boolean needsFlatLightUpdate = true;
//    private boolean needsAoLightUpdate = true;
//    
//    public LazyBlockInfo(BlockColors colors)
//    {
//        super(colors);
//    }
//    
//    /**
//     * Consolidate multiple calls to {@link #reset()}, {@link #setBlockPos(net.minecraft.util.math.BlockPos)},
//     * {@link #setState(net.minecraft.block.state.IBlockState)} and {@link #setWorld(net.minecraft.world.IBlockAccess)}
//     */
//    public void prepare(IBlockAccess world, IBlockState state, BlockPos pos)
//    {
//        this.reset();
//        if(fastPrepare)
//        {
//            try
//            {
//                fdWorld.set(this, world);
//                fdState.set(this, state);
//                fdPos.set(this, pos);
//            }
//            catch (Exception e)
//            {
//                this.setBlockPos(pos);
//                this.setState(state);
//                this.setWorld(world);
//            }
//        }
//        else
//        {
//            this.setBlockPos(pos);
//            this.setState(state);
//            this.setWorld(world);
//        }
//    }
//    
//    @Override
//    public void reset()
//    {
//        super.reset();
//        this.needsAoLightUpdate = true;
//        this.needsFlatLightUpdate = true;
//    }
//
//    @Override
//    public float[][][] getAo()
//    {
//        if(this.needsAoLightUpdate)
//        {
//            this.needsAoLightUpdate = false;
//            this.updateLightMatrix();
//        }
//        return super.getAo();
//    }
//
//    @Override
//    public int[] getPackedLight()
//    {
//        if(this.needsFlatLightUpdate)
//        {
//            this.needsFlatLightUpdate = false;
//            this.updateFlatLighting();
//        }
//        return super.getPackedLight();
//    }
//    
//    @Override
//    public void updateLightMatrix()
//    {
//        BlockPos blockPos = this.getBlockPos();
//        I world = this.getWorld();
//        
//        for(int x = 0; x <= 2; x++)
//        {
//            for(int y = 0; y <= 2; y++)
//            {
//                for(int z = 0; z <= 2; z++)
//                {
//                    BlockPos pos = blockPos.add(x - 1, y - 1, z - 1);
//                    IBlockState state = world.getBlockState(pos);
//                    t[x][y][z] = state.getLightOpacity(world, pos) < 15;
//                    int brightness = state.getPackedLightmapCoords(world, pos);
//                    s[x][y][z] = (brightness >> 0x14) & 0xF;
//                    b[x][y][z] = (brightness >> 0x04) & 0xF;
//                    ao[x][y][z] = state.getAmbientOcclusionLightValue();
//                }
//            }
//        }
//        for(EnumFacing side : SIDES)
//        {
//            if(!state.doesSideBlockRendering(world, blockPos, side))
//            {
//                int x = side.getXOffset() + 1;
//                int y = side.getYOffset() + 1;
//                int z = side.getZOffset() + 1;
//                s[x][y][z] = Math.max(s[1][1][1] - 1, s[x][y][z]);
//                b[x][y][z] = Math.max(b[1][1][1] - 1, b[x][y][z]);
//            }
//        }
//        for(int x = 0; x < 2; x++)
//        {
//            for(int y = 0; y < 2; y++)
//            {
//                for(int z = 0; z < 2; z++)
//                {
//                    int x1 = x * 2;
//                    int y1 = y * 2;
//                    int z1 = z * 2;
//
//                    int     sxyz = s[x1][y1][z1];
//                    int     bxyz = b[x1][y1][z1];
//                    boolean txyz = t[x1][y1][z1];
//
//                    int     sxz = s[x1][1][z1], sxy = s[x1][y1][1], syz = s[1][y1][z1];
//                    int     bxz = b[x1][1][z1], bxy = b[x1][y1][1], byz = b[1][y1][z1];
//                    boolean txz = t[x1][1][z1], txy = t[x1][y1][1], tyz = t[1][y1][z1];
//
//                    int     sx = s[x1][1][1], sy = s[1][y1][1], sz = s[1][1][z1];
//                    int     bx = b[x1][1][1], by = b[1][y1][1], bz = b[1][1][z1];
//                    boolean tx = t[x1][1][1], ty = t[1][y1][1], tz = t[1][1][z1];
//
//                    skyLight  [0][x][y][z] = combine(sx, sxz, sxy, txz || txy ? sxyz : sx,
//                                                     tx, txz, txy, txz || txy ? txyz : tx);
//                    blockLight[0][x][y][z] = combine(bx, bxz, bxy, txz || txy ? bxyz : bx,
//                                                     tx, txz, txy, txz || txy ? txyz : tx);
//
//                    skyLight  [1][x][y][z] = combine(sy, sxy, syz, txy || tyz ? sxyz : sy,
//                                                     ty, txy, tyz, txy || tyz ? txyz : ty);
//                    blockLight[1][x][y][z] = combine(by, bxy, byz, txy || tyz ? bxyz : by,
//                                                     ty, txy, tyz, txy || tyz ? txyz : ty);
//
//                    skyLight  [2][x][y][z] = combine(sz, syz, sxz, tyz || txz ? sxyz : sz,
//                                                     tz, tyz, txz, tyz || txz ? txyz : tz);
//                    blockLight[2][x][y][z] = combine(bz, byz, bxz, tyz || txz ? bxyz : bz,
//                                                     tz, tyz, txz, tyz || txz ? txyz : tz);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void updateFlatLighting()
//    {
//        full = state.isFullCube();
//        packed[0] = state.getPackedLightmapCoords(world, blockPos);
//
//        for (EnumFacing side : SIDES)
//        {
//            int i = side.ordinal() + 1;
//            packed[i] = state.getPackedLightmapCoords(world, blockPos.offset(side));
//        }
//    }
//}
