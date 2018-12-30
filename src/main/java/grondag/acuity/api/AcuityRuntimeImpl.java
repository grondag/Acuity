package grondag.acuity.api;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import grondag.acuity.Acuity;
import grondag.acuity.buffering.MappedBufferStore;
import grondag.acuity.fermion.config.Localization;
import grondag.acuity.hooks.PipelineHooks;
import grondag.acuity.pipeline.PipelineShaderManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AcuityRuntimeImpl extends AcuityRuntime
{
    public static final AcuityRuntimeImpl INSTANCE = new AcuityRuntimeImpl();
    
    public static void initialize()
    {
        AcuityRuntime.instance = INSTANCE;
    }
    
    private ArrayList<WeakReference<AcuityListener>> listeners = new ArrayList<WeakReference<AcuityListener>>();
    
    private AcuityRuntimeImpl() {};
    
    
    @Override
    public final PipelineManager getPipelineManager()
    {
        return PipelineManagerImpl.INSTANCE;
    }

    @Override
    public final boolean isAcuityEnabled()
    {
        return Acuity.isModEnabled();
    }
    
    public void forceReload()
    {
        Acuity.INSTANCE.getLog().info(Localization.translate("misc.info_reloading"));
        Acuity.recomputeEnabledStatus();
        PipelineShaderManager.INSTANCE.forceReload();
        PipelineManagerImpl.INSTANCE.forceReload();
        PipelineHooks.forceReload();
        MappedBufferStore.forceReload();
        forEachListener(c -> c.onRenderReload());
    }

    @Override
    public void registerListener(AcuityListener listener)
    {
        this.listeners.add(new WeakReference<AcuityListener>(listener));
    }
    
    public void forEachListener(Consumer<AcuityListener> c)
    {
        Iterator<WeakReference<AcuityListener>> it = this.listeners.iterator();
        while(it.hasNext())
        {
            WeakReference<AcuityListener> ref = it.next();
            AcuityListener listener = ref.get();
            if(listener == null)
                it.remove();
            else
                c.accept(listener);
        }
    }
}
