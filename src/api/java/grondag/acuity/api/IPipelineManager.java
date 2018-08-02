package grondag.acuity.api;

import javax.annotation.Nullable;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelineManager
{
    /**
     * Will return null if pipeline limit would be exceeded.
     */
    @Nullable
    IRenderPipeline getOrCreatePipeline(
            TextureFormat textureFormat, 
            IProgram program, 
            @Nullable IPipelineCallback callback);  
    
    /**
     * Use when you want standard rendering.
     */
    IRenderPipeline getDefaultPipeline(TextureFormat textureFormat);

    IRenderPipeline getWaterPipeline();

    IRenderPipeline getLavaPipeline();

    IRenderPipeline getPipelineByIndex(int index);
    
}
