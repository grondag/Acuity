package grondag.render_hooks.core;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.world.World;

public class CompoundListedRenderChunk extends ListedRenderChunk
{
    public CompoundListedRenderChunk(World worldIn, RenderGlobal renderGlobalIn, int index)
    {
        super(worldIn, renderGlobalIn, index);
    }

    @Override
    public void deleteGlResources()
    {
        // TODO delete compound lists
        super.deleteGlResources();
    }
}
