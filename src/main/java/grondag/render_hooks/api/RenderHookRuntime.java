package grondag.render_hooks.api;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderHookRuntime;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderHookRuntime implements IRenderHookRuntime
{
    // UGLY: hide?
    public static final RenderHookRuntime INSTANCE = new RenderHookRuntime();
    

    private RenderHookRuntime() {};
    
    
    @Override
    public final IPipelineManager getPipelineManager()
    {
        return PipelineManager.INSTANCE;
    }

    @Override
    public final boolean isRenderHooksEnabled()
    {
        return RenderHooks.isModEnabled();
    }

    @Override
    public final IPipelineShaderManager getShaderManager()
    {
        return PipelineShaderManager.INSTANCE;
    }
    
    public final IProgramManager getProgramManager()
    {
        return ProgramManager.INSTANCE;
    }
    
    public void forceReload()
    {
        // TODO: remove or improve
        System.out.println("RenderHooks force reload");
        PipelineShaderManager.INSTANCE.forceReload();
        ProgramManager.INSTANCE.forceReload();
        
    }
}
