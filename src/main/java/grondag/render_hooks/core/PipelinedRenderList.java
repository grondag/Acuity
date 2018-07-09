package grondag.render_hooks.core;

import net.minecraft.client.renderer.RenderList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelinedRenderList extends RenderList
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        super.renderChunkLayer(layer);
        
        //TODO: render stuff
    }
}
