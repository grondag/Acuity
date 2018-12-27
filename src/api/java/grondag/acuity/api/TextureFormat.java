package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum TextureFormat
{
    SINGLE,
    DOUBLE,
    TRIPLE;

    public int layerCount()
    {
        return this.ordinal() + 1;
    }
}
