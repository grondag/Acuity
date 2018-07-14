package grondag.render_hooks.api;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.PipelineManager;
import grondag.render_hooks.api.RenderHookRuntime;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderHookRuntimeImpl extends RenderHookRuntime
{
    // UGLY: hide?
    public static final RenderHookRuntimeImpl INSTANCE = new RenderHookRuntimeImpl();
    

    private RenderHookRuntimeImpl() {};
    
    
    @Override
    public final PipelineManager getPipelineManager()
    {
        return PipelineManagerImpl.INSTANCE;
    }

    @Override
    public final boolean isRenderHooksEnabled()
    {
        return RenderHooks.isModEnabled();
    }

    @Override
    public final PipelineShaderManager getShaderManager()
    {
        return PipelineShaderManagerImpl.INSTANCE;
    }
    
    public final IProgramManager getProgramManager()
    {
        return ProgramManager.INSTANCE;
    }
    
    public void forceReload()
    {
        // TODO: remove or improve
        System.out.println("RenderHooks force reload");
        PipelineShaderManagerImpl.INSTANCE.forceReload();
        ProgramManager.INSTANCE.forceReload();
        
    }
}
