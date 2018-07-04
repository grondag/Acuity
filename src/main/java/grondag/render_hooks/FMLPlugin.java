package grondag.render_hooks;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE)
public class FMLPlugin implements IFMLLoadingPlugin
{
    public static boolean runtimeDeobfEnabled = false;
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{"grondag.render_hooks.ASMTransformer"};
    }

    @Override
    public String getModContainerClass()
    {
        return "grondag.render_hooks.RenderHooks";
    }

    @Override
    @Nullable
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(@Nullable Map<String, Object> data)
    {
        //NOOP 
    }

    @Override
    @Nullable
    public String getAccessTransformerClass()
    {
        return null;
    }

}