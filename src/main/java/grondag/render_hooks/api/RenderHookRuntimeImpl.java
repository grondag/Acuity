package grondag.render_hooks.api;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.PipelineManager;
import grondag.render_hooks.api.RenderHookRuntime;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderHookRuntimeImpl extends RenderHookRuntime
{
    
    //TODO: hide
    public static final RenderHookRuntimeImpl INSTANCE = new RenderHookRuntimeImpl();
    
    private final  PipelineManagerImpl pipelineManager = new PipelineManagerImpl();
    
    private final  PipelineShaderManagerImpl shaderManager = new PipelineShaderManagerImpl();

    private RenderHookRuntimeImpl() {};
    
    
    @Override
    public final PipelineManager getPipelineManager()
    {
        return pipelineManager;
    }

    @Override
    public final boolean isRenderHooksEnabled()
    {
        return RenderHooks.isModEnabled();
    }

    @Override
    public final PipelineShaderManager getShaderManager()
    {
        return this.shaderManager;
    }
}
