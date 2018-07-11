package grondag.render_hooks.core;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import grondag.render_hooks.api.PipelineManager;
import grondag.render_hooks.api.RenderPipeline;
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
    private RenderPipeline[] pipelines = new RenderPipeline[PipelineManager.MAX_PIPELINES];
    private int[] pipelineBufferIds = new int[PipelineManager.MAX_PIPELINES];
    private int[] pipelineCounts = new int[PipelineManager.MAX_PIPELINES];
    
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
    
    public void uploadBuffer(RenderPipeline pipeline, ByteBuffer data)
    {
        pipelines[slotsInUse] = pipeline;
        
        final int glId = getAvailableBufferId();
        pipelineBufferIds[slotsInUse] = glId;

        pipelineCounts[slotsInUse] = data.limit() / pipeline.vertexFormat().getNextOffset();
        // shouldn't matter normally but if have partial ASM failure could prevent a break
        if(pipeline.getIndex() == PipelineManager.VANILLA_MC_PIPELINE_INDEX)
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
            final RenderPipeline p  = this.pipelines[i];
            
            //TODO: do this based on vertex format
            //     and reuse a vertex attribute array per format
            GlStateManager.glVertexPointer(3, 5126, 28, 0);
            GlStateManager.glColorPointer(4, 5121, 28, 12);
            GlStateManager.glTexCoordPointer(2, 5126, 28, 16);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glTexCoordPointer(2, 5122, 28, 24);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            
            p.preDraw();
            GlStateManager.glDrawArrays(GL11.GL_QUADS, 0, this.pipelineCounts[i]);
            p.postDraw();
        }
    }
}
