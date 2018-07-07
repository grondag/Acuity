package grondag.render_hooks.api;

import java.util.function.Consumer;

import net.minecraft.client.renderer.block.model.IBakedModel;

public interface IPipelinedBakedModel extends IBakedModel
{
    public void processQuads(Consumer<IPipelinedBakedQuad> quadConsumer);
}
