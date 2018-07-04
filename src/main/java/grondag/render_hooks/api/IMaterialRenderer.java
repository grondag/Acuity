package grondag.render_hooks.api;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;

public interface IMaterialRenderer
{
    public void initializeBuffer(BufferBuilder buffer);
    
    public void preDraw();
    
    public void postDraw();
    
    /**
     * GL mode  format to be used for this material.
     * Defaults to GL_QUADS
     */
    public default int glMode()
    {
        return GL11.GL_QUADS;
    }
    
    /**
     * Vertex buffer format to be used for this material.
     * Defaults to BLOCK format.
     */
    public default VertexFormat vertexFormat()
    {
        return DefaultVertexFormats.BLOCK;
    }
}
