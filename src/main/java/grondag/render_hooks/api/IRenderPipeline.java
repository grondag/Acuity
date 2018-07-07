package grondag.render_hooks.api;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRenderPipeline
{
    public void initializeBuffer(BufferBuilder buffer);
    
    public void preDraw();
    
    public void postDraw();
    
    /**
     * GL mode  format to be used for this pipeline.
     * Defaults to GL_QUADS
     */
    public default int glMode()
    {
        return GL11.GL_QUADS;
    }
    
    /**
     * Target vertex buffer format to be used for this pipeline.
     * Defaults to BLOCK format.
     */
    public default VertexFormat vertexFormat()
    {
        return DefaultVertexFormats.BLOCK;
    }

    public int getIndex();

    public void assignIndex(int n);
}
