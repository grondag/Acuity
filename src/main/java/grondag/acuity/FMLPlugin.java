package grondag.acuity;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE)
public class FMLPlugin implements IFMLLoadingPlugin
{
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{"grondag.acuity.ASMTransformer"};
    }

    @SuppressWarnings("null")
    @Override
    public String getModContainerClass()
    {
        return null;
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