package grondag.render_hooks.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRenderPipeline
{
    /**
     * For internal use.<br>
     */
    int getIndex();
    
    /**
     * Executes callback if there is one.
     * For internal use.<br>
     */
    void preDraw();
    
    /**
     * Executes callback if there is one.
     * For internal use.<br>
     */
    void postDraw();

}
