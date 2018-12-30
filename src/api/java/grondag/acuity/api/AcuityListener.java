package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Implement and register to receive notification of Acuity events.  
 *
 */
@Environment(EnvType.CLIENT)
public interface AcuityListener
{
    /**
     * Will only be called when the status changes, so you may reliably
     * infer the previous status is the opposite of the new status.
     */
    public default void onAcuityStatusChange(boolean newEnabledStatus) {};
    
    /**
     * Called when rendered chunks, shaders, etc. are reloaded, due to a
     * configuration change, resource pack change, or user pressing F3 + A;
     */
    public default void onRenderReload() {};
}
