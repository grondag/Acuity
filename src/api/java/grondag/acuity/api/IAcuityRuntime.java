package grondag.acuity.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IAcuityRuntime
{
    /**
     * Get this to register your pipelines and access the vanilla pipeline.
     */
    IPipelineManager getPipelineManager();
    
    IPipelineShaderManager getShaderManager();
    
    /**
     * Will be false if any part of ASM modifications failed or
     * if user has disabled Acuity in configuration.
     */
    boolean isAcuityEnabled();
}
