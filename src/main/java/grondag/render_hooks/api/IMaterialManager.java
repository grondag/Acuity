package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IMaterialManager
{
    public static final int MAX_MATERIAL_COUNT = 1024;
    
    /**
     * Will return -1 if material limit would be exceeded.
     */
    public int createMaterial(@Nonnull IMaterialRenderer material);
}
