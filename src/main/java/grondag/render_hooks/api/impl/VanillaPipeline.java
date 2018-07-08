package grondag.render_hooks.api.impl;

import javax.annotation.Nonnull;

import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.util.BlockRenderLayer;

public class VanillaPipeline implements IRenderPipeline
{
    private int index;
    private final BlockRenderLayer layer;
    
    public VanillaPipeline(@Nonnull BlockRenderLayer layer)
    {
        this.layer = layer;
    }
    
    @Override
    public void preDraw()
    {
        // NOOP        
    }

    @Override
    public void postDraw()
    {
        // NOOP
    }

    @Override
    public int getIndex()
    {
        return index;
    }

    @Override
    public void assignIndex(int index)
    {
        this.index = index;
    }

    @Override
    public BlockRenderLayer renderLayer()
    {
        return this.layer;
    }

}
