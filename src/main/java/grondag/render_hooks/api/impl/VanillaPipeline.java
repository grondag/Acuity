package grondag.render_hooks.api.impl;

import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VanillaPipeline implements IRenderPipeline
{
    private int index;
    
    @Override
    public void preDraw()
    {
        GlStateManager.glVertexPointer(3, 5126, 28, 0);
        GlStateManager.glColorPointer(4, 5121, 28, 12);
        GlStateManager.glTexCoordPointer(2, 5126, 28, 16);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glTexCoordPointer(2, 5122, 28, 24);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public void postDraw()
    {
        // NOOP
    }

    @Override
    public int getIndex()
    {
        return index;
    }

    @Override
    public void assignIndex(int index)
    {
        this.index = index;
    }

    @Override
    public void preDrawList()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void postDrawList()
    {
        // TODO Auto-generated method stub
        
    }
}
