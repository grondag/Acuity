package grondag.render_hooks.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRenderHookRuntime
{
    public IMaterialManager getMaterialManager();
}
