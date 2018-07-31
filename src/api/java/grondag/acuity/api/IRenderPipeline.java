package grondag.acuity.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Type-safe reference to a rendering pipeline.
 */
@SideOnly(Side.CLIENT)
public interface IRenderPipeline
{
    int getIndex();
    
    TextureFormat textureFormat();
}
