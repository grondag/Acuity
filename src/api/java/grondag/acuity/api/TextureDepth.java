package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum TextureDepth
{
    SINGLE,
    DOUBLE,
    TRIPLE;

    public final int layerCount()
    {
        return this.ordinal() + 1;
    }
}
