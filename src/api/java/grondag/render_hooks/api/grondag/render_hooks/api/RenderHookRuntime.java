package grondag.render_hooks.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class RenderHookRuntime
{
    RenderHookRuntime() {}
    
    /**
     * Get this to register your pipelines and access the vanilla pipeline.
     */
    public abstract PipelineManager getPipelineManager();
    
    public abstract PipelineShaderManager getShaderManager();
    
    /**
     * Will be false if any part of ASM modifications failed or
     * if user has disabled RenderHooks in configuration.
     */
    public abstract boolean isRenderHooksEnabled();
}
