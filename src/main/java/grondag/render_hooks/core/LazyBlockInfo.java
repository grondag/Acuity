package grondag.render_hooks.core;

import net.minecraft.client.renderer.color.BlockColors;
import net.minecraftforge.client.model.pipeline.BlockInfo;

public class LazyBlockInfo extends BlockInfo
{
    private boolean needsFlatLightUpdate = true;
    private boolean needsAoLightUpdate = true;
    
    public LazyBlockInfo(BlockColors colors)
    {
        super(colors);
    }

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
