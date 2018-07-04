package grondag.render_hooks.api.impl;

import grondag.render_hooks.api.IMaterialManager;
import grondag.render_hooks.api.IRenderHookRuntime;

public class RenderHookRuntime implements IRenderHookRuntime
{
    public final  MaterialManager materialManager = new MaterialManager();

    @Override
    public IMaterialManager getMaterialManager()
    {
        return materialManager;
    }

}
