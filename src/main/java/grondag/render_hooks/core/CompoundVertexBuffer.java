package grondag.render_hooks.core;

import static grondag.render_hooks.api.PipelineVertextFormatElements.BASE_RGBA_4UB;
import static grondag.render_hooks.api.PipelineVertextFormatElements.BASE_TEX_2F;
import static grondag.render_hooks.api.PipelineVertextFormatElements.LIGHTMAPS_4UB;
import static grondag.render_hooks.api.PipelineVertextFormatElements.NORMAL_AO_4B;
import static grondag.render_hooks.api.PipelineVertextFormatElements.POSITION_3F;
import static grondag.render_hooks.api.PipelineVertextFormatElements.SECONDARY_RGBA_4UB;
import static grondag.render_hooks.api.PipelineVertextFormatElements.SECONDARY_TEX_2F;
import static grondag.render_hooks.api.PipelineVertextFormatElements.TERTIARY_RGBA_4UB;
import static grondag.render_hooks.api.PipelineVertextFormatElements.TERTIARY_TEX_2F;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.PipelineVertexFormat;
import grondag.render_hooks.api.IRenderPipeline;
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
    private int slotsInUse = 0;
    private int nextStartIndex = 0;
    private int currentAllocationBytes = 0;
    
    private IRenderPipeline[] pipelines = new IRenderPipeline[IPipelineManager.MAX_PIPELINES];
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
    
    public void uploadBuffer(IRenderPipeline pipeline, ByteBuffer data)
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
            final IRenderPipeline p  = this.pipelines[i];
            final int offset = this.pipelineBufferOffset[i];
            
            setupVertexAttributes(p.pipelineVertexFormat(), offset);
            p.preDraw();
            GlStateManager.glDrawArrays(GL11.GL_QUADS, offset, this.pipelineCounts[i]);
            p.postDraw();
        }
    }
    
    private static @Nullable PipelineVertexFormat lastEnabledFormat = null;
    
    private void setupVertexAttributes(PipelineVertexFormat format, int bufferOffset)
    {
        final int stride = format.vertexFormat.getNextOffset();
        
        GlStateManager.glVertexPointer(3, POSITION_3F.getType().getGlConstant(), stride, bufferOffset + 0);
        GlStateManager.glColorPointer(4, BASE_RGBA_4UB.getType().getGlConstant(), stride, bufferOffset + 12);
        GlStateManager.glTexCoordPointer(2, BASE_TEX_2F.getType().getGlConstant(), stride, bufferOffset + 16);
        GL20.glVertexAttribPointer(0, 4, NORMAL_AO_4B.getType().getGlConstant(), true, stride, bufferOffset + 24);
        GL20.glVertexAttribPointer(1, 4, LIGHTMAPS_4UB.getType().getGlConstant(), false, stride, bufferOffset + 28);
        
        switch(format)
        {
        case SINGLE:
            if(lastEnabledFormat != PipelineVertexFormat.SINGLE)
            {
                GL20.glDisableVertexAttribArray(2);
                GL20.glDisableVertexAttribArray(3);
                if(lastEnabledFormat == PipelineVertexFormat.TRIPLE)
                {
                    GL20.glDisableVertexAttribArray(4);
                    GL20.glDisableVertexAttribArray(5);
                }
            }
            break;
            
        case DOUBLE:
            if(lastEnabledFormat == PipelineVertexFormat.SINGLE)
            {
                GL20.glEnableVertexAttribArray(2);
                GL20.glEnableVertexAttribArray(3);
            }
            else if(lastEnabledFormat == PipelineVertexFormat.TRIPLE)
            {
                GL20.glDisableVertexAttribArray(4);
                GL20.glDisableVertexAttribArray(5);
            }
            GL20.glVertexAttribPointer(2, 1, SECONDARY_RGBA_4UB.getType().getGlConstant(), false, stride, bufferOffset + 32);
            GL20.glVertexAttribPointer(3, 4, SECONDARY_TEX_2F.getType().getGlConstant(), false, stride, bufferOffset + 36);
            break;
            
        case TRIPLE:
            if(lastEnabledFormat == PipelineVertexFormat.SINGLE)
            {
                GL20.glEnableVertexAttribArray(2);
                GL20.glEnableVertexAttribArray(3);
            }
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            
            GL20.glVertexAttribPointer(2, 1, SECONDARY_RGBA_4UB.getType().getGlConstant(), false, stride, bufferOffset + 32);
            GL20.glVertexAttribPointer(3, 4, SECONDARY_TEX_2F.getType().getGlConstant(), false, stride, bufferOffset + 36);
            GL20.glVertexAttribPointer(4, 1, TERTIARY_RGBA_4UB.getType().getGlConstant(), false, stride, bufferOffset + 44);
            GL20.glVertexAttribPointer(5, 4, TERTIARY_TEX_2F.getType().getGlConstant(), false, stride, bufferOffset + 48);
            break;
            
        default:
            throw new UnsupportedOperationException("Bad pipeline vertex format.");
        
        }
        
    }
}
