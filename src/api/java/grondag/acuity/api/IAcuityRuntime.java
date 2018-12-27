package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface IAcuityRuntime
{
    /**
     * Get this to register your pipelines and access the built-in pipelines.
     */
    IPipelineManager getPipelineManager();
    
    /**
     * Will be false if any part of ASM modifications failed or
     * if user has disabled Acuity in configuration.
     */
    boolean isAcuityEnabled();
    
    /**
     * Use if you need callbacks for status changes.
     * Holds a weak reference, so no need to remove listeners that fall out of scope.
     */
    void registerListener(IAcuityListener lister);

}
