package grondag.render_hooks.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRenderHookRuntime
{
    /**
     * Get this to register your pipelines and access the vanilla pipeline.
     */
    IPipelineManager getPipelineManager();
    
    IPipelineShaderManager getShaderManager();
    
    /**
     * Will be false if any part of ASM modifications failed or
     * if user has disabled RenderHooks in configuration.
     */
    boolean isRenderHooksEnabled();
}
