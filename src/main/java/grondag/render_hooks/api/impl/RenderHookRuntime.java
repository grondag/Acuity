package grondag.render_hooks.api.impl;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderHookRuntime;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderHookRuntime implements IRenderHookRuntime
{
    public final  PipelineManager pipelineManager = new PipelineManager();

    @Override
    public IPipelineManager getPipelineManager()
    {
        return pipelineManager;
    }

    @Override
    public boolean isRenderHooksEnabled()
    {
        return RenderHooks.isModEnabled();
    }
}
