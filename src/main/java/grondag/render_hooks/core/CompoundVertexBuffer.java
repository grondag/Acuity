package grondag.render_hooks.core;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import grondag.render_hooks.api.RenderPipeline;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Multi-pipeline version of VertexBuffer.<p>
 * 
 * RenderChunk keeps a separate VertexBuffer for each BlockRenderLayer,
 * so we can assume that all pipeline IDs are for a single layer.
 */
@SideOnly(Side.CLIENT)
public class CompoundVertexBuffer extends VertexBuffer
{
    
    private @Nullable VertexPackingList vertexPackingList;
    
    public CompoundVertexBuffer(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);
    }

    public void upload(ByteBuffer buffer, VertexPackingList packing)
    {
        this.vertexPackingList = packing;
        buffer.position(0);
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }
    
    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
    }

//    static int totalSlots;
//    static int runCount;
    /**
     * Renders all uploaded vbos.
     * Layer is passed in because was easier (ASM-wise) to not track this here.
     * Must know layer to look up pipelines.
     */
    public void renderChunk()
    {
        final VertexPackingList packing = this.vertexPackingList;
        if(packing == null || packing.size() == 0) return;
        
//        totalSlots += this.slotsInUse;
//        if(++runCount >=  2000)
//        {
//            RenderHooks.INSTANCE.getLog().info("Average slots per renderChunk() = " + (float) totalSlots / runCount);
//            totalSlots = 0;
//            runCount = 0;
//        }
        
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        
        packing.forEach((RenderPipeline p, int offset, int vertexCount) ->
        {
            GlStateManager.glVertexPointer(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), p.piplineVertexFormat().stride, offset);
            p.piplineVertexFormat().setupAttributes(offset);
            p.preDraw();
            GlStateManager.glDrawArrays(GL11.GL_QUADS, offset, vertexCount);
            p.postDraw();
        });
    }
}
