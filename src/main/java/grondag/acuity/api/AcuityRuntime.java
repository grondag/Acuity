package grondag.acuity.api;

import java.util.ArrayList;
import java.util.function.Consumer;

import grondag.acuity.Acuity;
import grondag.acuity.api.IAcuityRuntime;
import grondag.acuity.api.IPipelineManager;
import grondag.acuity.api.IPipelineShaderManager;
import grondag.acuity.api.IProgramManager;
import grondag.acuity.core.PipelineHooks;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SuppressWarnings("deprecation")
@SideOnly(Side.CLIENT)
public final class AcuityRuntime implements IAcuityRuntime
{
    public static final AcuityRuntime INSTANCE = new AcuityRuntime();

    private ArrayList<IAcuityListener> listeners = new ArrayList<>();
    
    private AcuityRuntime() {};
    
    
    @Override
    public final IPipelineManager getPipelineManager()
    {
        return PipelineManager.INSTANCE;
    }

    @Override
    public final boolean isAcuityEnabled()
    {
        return Acuity.isModEnabled();
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
        Acuity.INSTANCE.getLog().info(I18n.translateToLocal("misc.info_reloading"));
        PipelineShaderManager.INSTANCE.forceReload();
        ProgramManager.INSTANCE.forceReload();
        PipelineManager.INSTANCE.forceReload();
        PipelineHooks.forceReload();
    }

    @Override
    public void registerListener(IAcuityListener listener)
    {
        this.listeners.add(listener);
    }
    
    public void forEachListener(Consumer<IAcuityListener> c)
    {
        this.listeners.forEach(c);
    }
}
