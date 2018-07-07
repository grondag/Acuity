package grondag.render_hooks.core;

import java.util.function.Consumer;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelinedBakedQuad;
import grondag.render_hooks.api.IRenderPipeline;
import grondag.render_hooks.api.impl.PipelineManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.client.model.pipeline.QuadGatheringTransformer;

/**
 * A version of Forge vertex lighter that supports multiple render paths in same quad stream.
 *
 */
public abstract class PipelinedVertexLighter extends QuadGatheringTransformer implements Consumer<IPipelinedBakedQuad>
{
    private int tint = -1;
    private boolean diffuse = true;

    private final IRenderPipeline pipeline;
    
    protected int posIndex = -1;
    protected int normalIndex = -1;
    protected int colorIndex = -1;
    protected int lightmapIndex = -1;
    
    public abstract BlockInfo getBlockInfo();
    
    protected PipelinedVertexLighter(IRenderPipeline pipeline)
    {
        this.pipeline = pipeline;
    }
    
    @Override
    public void setQuadTint(int tint)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setQuadOrientation(EnumFacing orientation)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setApplyDiffuseLighting(boolean diffuse)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setTexture(TextureAtlasSprite texture)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void processQuad()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void accept(IPipelinedBakedQuad t)
    {
        
    }

}
