package grondag.render_hooks.core;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
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
    /**
     * Holds all allocated gl buffer names claimed in addition to the one claimed by super 
     */
    private IntArrayList glBufferIds = new IntArrayList();
    
    private int slotsInUse = 0;
    private IRenderPipeline[] pipelines = new IRenderPipeline[IPipelineManager.MAX_PIPELINES];
    private int[] pipelineBufferIds = new int[IPipelineManager.MAX_PIPELINES];
    private int[] pipelineCounts = new int[IPipelineManager.MAX_PIPELINES];
    
    public CompoundVertexBuffer(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);
        if(this.glBufferId >= 0) 
            glBufferIds.add(this.glBufferId);
    }
    
    private int getAvailableBufferId()
    {
        int result;
        if(slotsInUse < glBufferIds.size())
        {
            result = glBufferIds.getInt(slotsInUse);
        }
        else
        {
            result = OpenGlHelper.glGenBuffers();
            glBufferIds.add(result);
        }
        return result;
    }

    public void prepareForUpload()
    {
        this.slotsInUse = 0;
    }
    
    public void uploadBuffer(IRenderPipeline pipeline, ByteBuffer data)
    {
        pipelines[slotsInUse] = pipeline;
        
        final int glId = getAvailableBufferId();
        pipelineBufferIds[slotsInUse] = glId;

        pipelineCounts[slotsInUse] = data.limit() / pipeline.vertexFormat().getNextOffset();
        // shouldn't matter normally but if have partial ASM failure could prevent a break
        if(pipeline.getIndex() == IPipelineManager.VANILLA_MC_PIPELINE_INDEX)
            this.count = pipelineCounts[0];

        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, glId);
        OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, data, 35044);
        
        slotsInUse++;
    }
    
    public void completeUpload()
    {
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }
    
    @Override
    public void deleteGlBuffers()
    {
        for(int i : this.glBufferIds)
        {
            if(i >= 0)
                OpenGlHelper.glDeleteBuffers(i);
        }
        this.glBufferIds.clear();
        this.glBufferId = -1;
        this.slotsInUse = 0;
    }

    /**
     * Renders all uploaded vbos.
     * Layer is passed in because was easier (ASM-wise) to not track this here.
     * Must know layer to look up pipelines.
     */
    public void renderChunk()
    {
        for(int i = 0; i < this.slotsInUse; i++)
        {
            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, pipelineBufferIds[i]);
            final IRenderPipeline p  = this.pipelines[i];
            p.preDraw();
            GlStateManager.glDrawArrays(GL11.GL_QUADS, 0, this.pipelineCounts[i]);
            p.postDraw();
        }
    }
}
