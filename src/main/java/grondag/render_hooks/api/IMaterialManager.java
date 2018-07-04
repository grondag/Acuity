package grondag.render_hooks.api;

import javax.annotation.Nonnull;

public interface IMaterialManager
{
    public static final int MAX_MATERIAL_COUNT = 1024;
    
    /**
     * Will return -1 if material limit would be exceeded.
     */
    public int createMaterial(@Nonnull IMaterialRenderer material);
}
