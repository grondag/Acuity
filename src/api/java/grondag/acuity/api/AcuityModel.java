package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface AcuityModel
{
    public void produceQuads(AcuityVertexConsumer quadConsumer);
}
