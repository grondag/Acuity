package grondag.render_hooks.core;

import java.lang.reflect.Field;

import grondag.render_hooks.RenderHooks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.pipeline.BlockInfo;

@SuppressWarnings("null")
public class LazyBlockInfo extends BlockInfo
{
    // forge made parent fields private for whatever reasons...
    private static final Field fdWorld;
    private static final Field fdState;
    private static final Field fdPos;
    
    private static boolean fastPrepare;
    
    static
    {
        boolean isFast = false;
        Field world = null;
        Field state = null;
        Field pos = null;
        try
        {
            world = BlockInfo.class.getDeclaredField("world");
            world.setAccessible(true);
            state = BlockInfo.class.getDeclaredField("state");
            state.setAccessible(true);
            pos = BlockInfo.class.getDeclaredField("blockPos");
            pos.setAccessible(true);
            
            // confirm it works
            LazyBlockInfo testTarget = new LazyBlockInfo(null);
            world.set(testTarget, null);
            state.set(testTarget, null);
            pos.set(testTarget, null);
            
            isFast = true;
        }
        catch(Exception e)
        {
            RenderHooks.INSTANCE.getLog().error("Unable to access BlockInfo parent fields. Will use slow initialization.", e);
            
        }
        fastPrepare = isFast;
        fdWorld = world;
        fdPos  = pos;
        fdState = state;
        
    }
    
    private boolean needsFlatLightUpdate = true;
    private boolean needsAoLightUpdate = true;
    
    public LazyBlockInfo(BlockColors colors)
    {
        super(colors);
    }
    
    /**
     * Consolidate multiple calls to {@link #reset()}, {@link #setBlockPos(net.minecraft.util.math.BlockPos)},
     * {@link #setState(net.minecraft.block.state.IBlockState)} and {@link #setWorld(net.minecraft.world.IBlockAccess)}
     */
    public void prepare(IBlockAccess world, IBlockState state, BlockPos pos)
    {
        this.reset();
        if(fastPrepare)
        {
            try
            {
                fdWorld.set(this, world);
                fdState.set(this, state);
                fdPos.set(this, pos);
            }
            catch (Exception e)
            {
                this.setBlockPos(pos);
                this.setState(state);
                this.setWorld(world);
            }
        }
        else
        {
            this.setBlockPos(pos);
            this.setState(state);
            this.setWorld(world);
        }
    }
    
    // UGLY: In call stack looks like this is getting called more often than needs to be
    // Should profile and see if matters - probably not, doesn't do much, but is called per-block
    @Override
    public void reset()
    {
        super.reset();
        this.needsAoLightUpdate = true;
        this.needsFlatLightUpdate = true;
    }

    @Override
    public float[][][] getAo()
    {
        if(this.needsAoLightUpdate)
        {
            this.needsAoLightUpdate = false;
            this.updateLightMatrix();
        }
        return super.getAo();
    }

    @Override
    public int[] getPackedLight()
    {
        if(this.needsFlatLightUpdate)
        {
            this.needsFlatLightUpdate = false;
            this.updateFlatLighting();
        }
        return super.getPackedLight();
    }
}
