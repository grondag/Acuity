package grondag.acuity;

import java.util.Map;

import javax.annotation.Nullable;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE)
public class AcuityCore implements IFMLLoadingPlugin
{
    public AcuityCore()
    {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.acuity.json");
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
    }
    
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