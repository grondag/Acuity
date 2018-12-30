package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Governs how often pipeline shader uniform initializers are called.<p>
 * 
 * In all cases, initializers will only be called if the pipeline is activated
 * and the values are only uploaded if they have changed.
 */
@Environment(EnvType.CLIENT)
public enum UniformUpdateFrequency
{
    /**
     * Uniform initializer only called 1X after load or reload.
     */
    ON_LOAD,

    /**
     * Uniform initializer called 1X per game tick. (20X per second)
     */
    PER_TICK,
    
    /**
     * Uniform initializer called 1X per render frame. (Variable frequency.)
     */
    PER_FRAME
}
