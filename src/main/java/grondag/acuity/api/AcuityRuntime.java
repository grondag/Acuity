package grondag.acuity.api;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import grondag.acuity.Acuity;
import grondag.acuity.core.PipelineHooks;
import grondag.acuity.core.PipelineShaderManager;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AcuityRuntime implements IAcuityRuntime
{
    public static final AcuityRuntime INSTANCE = new AcuityRuntime();

    private ArrayList<WeakReference<IAcuityListener>> listeners = new ArrayList<WeakReference<IAcuityListener>>();
    
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
    
    public void forceReload()
    {
        Acuity.INSTANCE.getLog().info(I18n.translateToLocal("misc.info_reloading"));
        Acuity.recomputeEnabledStatus();
        PipelineShaderManager.INSTANCE.forceReload();
        PipelineManager.INSTANCE.forceReload();
        PipelineHooks.forceReload();
    }

    @Override
    public void registerListener(IAcuityListener listener)
    {
        this.listeners.add(new WeakReference<IAcuityListener>(listener));
    }
    
    public void forEachListener(Consumer<IAcuityListener> c)
    {
        Iterator<WeakReference<IAcuityListener>> it = this.listeners.iterator();
        while(it.hasNext())
        {
            WeakReference<IAcuityListener> ref = it.next();
            IAcuityListener listener = ref.get();
            if(listener == null)
                it.remove();
            else
                c.accept(listener);
        }
    }
}
