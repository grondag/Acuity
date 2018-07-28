package grondag.acuity.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelineCallback
{
    /**
     * Called after all other bindings are done.</br>
     * 
     * Use with extreme caution. If you change anything, change it back.
     */
    public void preDraw();

    /**
     * Called after draw calls and before any binding changes occur for the next pipeline.
     */
    public void postDraw();
    
}
