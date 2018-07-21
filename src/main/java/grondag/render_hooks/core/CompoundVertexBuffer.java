package grondag.render_hooks.core;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import grondag.render_hooks.api.IPipelineManager;
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
 * RenderChunk keeps a separate VerteXBuffer for each BlockRenderLayer,
 * so we can assume that all pipeline IDs are for a single layer.
 */
@SideOnly(Side.CLIENT)
public class CompoundVertexBuffer extends VertexBuffer
{
    private int slotsInUse = 0;
    private int nextStartIndex = 0;
    private int currentAllocationBytes = 0;
    
    private RenderPipeline[] pipelines = new RenderPipeline[IPipelineManager.MAX_PIPELINES];
    private int[] pipelineBufferOffset = new int[IPipelineManager.MAX_PIPELINES];
    private int[] pipelineCounts = new int[IPipelineManager.MAX_PIPELINES];
    
    public CompoundVertexBuffer(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);
    }

    public void prepareForUpload(int totalBytes)
    {
        this.slotsInUse = 0;
        this.nextStartIndex = 0;
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        if(totalBytes > this.currentAllocationBytes)
        {
            OpenGlHelperExt.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, totalBytes, GL15.GL_STATIC_DRAW);
            this.currentAllocationBytes = totalBytes;
        }
    }
    
    public void uploadBuffer(RenderPipeline pipeline, ByteBuffer data)
    {
        this.pipelines[slotsInUse] = pipeline;
        this.pipelineBufferOffset[slotsInUse] = this.nextStartIndex;
        pipelineCounts[slotsInUse] = data.limit() / pipeline.vertexFormat().getNextOffset();
        slotsInUse++;
        
        // shouldn't matter normally but if have partial ASM failure could prevent a break
        if(pipeline.getIndex() == IPipelineManager.VANILLA_MC_PIPELINE_INDEX)
            this.count = pipelineCounts[0];

        OpenGlHelperExt.glBufferSubData(OpenGlHelper.GL_ARRAY_BUFFER, this.nextStartIndex, data);
        
        this.nextStartIndex += data.limit();
        
    }
    
    public void completeUpload()
    {
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }
    
    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
        this.slotsInUse = 0;
        this.nextStartIndex = 0;
    }

    /**
     * Renders all uploaded vbos.
     * Layer is passed in because was easier (ASM-wise) to not track this here.
     * Must know layer to look up pipelines.
     */
    public void renderChunk()
    {
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        for(int i = 0; i < this.slotsInUse; i++)
        {
            final RenderPipeline p  = this.pipelines[i];
            final int offset = this.pipelineBufferOffset[i];
            p.piplineVertexFormat().setupAttributes(offset);
            p.preDraw();
            GlStateManager.glDrawArrays(GL11.GL_QUADS, offset, this.pipelineCounts[i]);
            p.postDraw();
        }
    }
}
