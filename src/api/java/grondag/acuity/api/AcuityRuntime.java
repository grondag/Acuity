package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class AcuityRuntime
{
    static AcuityRuntime instance = new AcuityRuntime() {};
    
    public static AcuityRuntime getInstance()
    {
        return instance;
    }
    
    /**
     * Get this to register your pipelines and access the built-in pipelines.
     */
    public PipelineManager getPipelineManager()
    {
        return null;
    }
    
    /**
     * Will be false if any part of ASM modifications failed or
     * if user has disabled Acuity in configuration.
     */
    public boolean isAcuityEnabled()
    {
        return false;
    }
    
    /**
     * Use if you need callbacks for status changes.
     * Holds a weak reference, so no need to remove listeners that fall out of scope.
     */
    public void registerListener(AcuityListener lister)
    {
        // NO OP
    }
}