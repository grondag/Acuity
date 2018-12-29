package grondag.acuity.mixin.extension;

import net.minecraft.client.util.math.Matrix4f;

public interface Matrix4fExt
{
    void setFromMatrix(Matrix4f source);

    float[] getComponents();
}
