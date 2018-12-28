package grondag.acuity.extension;

import net.minecraft.client.util.math.Matrix4f;

public interface AcuityMatrix4f
{
    void setFromMatrix(Matrix4f source);

    float[] getComponents();
}
