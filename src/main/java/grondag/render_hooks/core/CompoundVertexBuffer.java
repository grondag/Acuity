package grondag.render_hooks.core;

import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glColorPointer;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glNormalPointer;
import static org.lwjgl.opengl.GL11.glTexCoordPointer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.nio.ByteBuffer;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.PipelineManager;
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
    
    private RenderPipeline[] pipelines = new RenderPipeline[PipelineManager.MAX_PIPELINES];
    private int[] pipelineBufferOffset = new int[PipelineManager.MAX_PIPELINES];
    private int[] pipelineCounts = new int[PipelineManager.MAX_PIPELINES];
    
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
        if(pipeline.getIndex() == PipelineManager.VANILLA_MC_PIPELINE_INDEX)
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
        
        // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
        // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.

        
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        for(int i = 0; i < this.slotsInUse; i++)
        {
            final RenderPipeline p  = this.pipelines[i];
            final int offset = this.pipelineBufferOffset[i];
            
            // vertex       12
            // base color   3
            // ao           1
            // uv           8

            // skylight     1
            // blocklight   1
            // glow_0       1
            // normal       3
            // glow_1       1
            // glow_2       1
            // 32 bytes for regular
            
            // extra buffers - 12 each
            // color_1      4
            // uv_1         8
            
            
            // vertex       12
            // uv           8
            // lightmap     4
            // color        4
            
            
            
            GlStateManager.glVertexPointer(3, 5126, 28, 0);
            GlStateManager.glColorPointer(4, 5121, 28, 12);
            GlStateManager.glTexCoordPointer(2, 5126, 28, 16);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glTexCoordPointer(2, 5122, 28, 24);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            
            setupVertexAttributes(p.vertexFormat(), offset);
            p.preDraw();
            GlStateManager.glDrawArrays(GL11.GL_QUADS, offset, this.pipelineCounts[i]);
            p.postDraw();
        }
    }
    
    private void setupVertexAttributes(VertexFormat format, int bufferOffset)
    {
        final int stride = format.getNextOffset();
        final List<VertexFormatElement> elements = format.getElements();
        final int size = elements.size();
        for (int j = 0; j < size; ++j)
        {
            VertexFormatElement attr = elements.get(j);
            
            int count = attr.getElementCount();
            int constant = attr.getType().getGlConstant();
            int genericIndex = 0;
            
            switch(attr.getUsage())
            {
                case POSITION:
                    GlStateManager.glVertexPointer(count, constant, stride, bufferOffset + format.getOffset(j));
                    GlStateManager.glEnableClientState(GL_VERTEX_ARRAY);
                    break;
                    
                case NORMAL:
                    if(count != 3)
                    {
                        throw new IllegalArgumentException("Normal attribute should have the size 3: " + attr);
                    }
                    glNormalPointer(constant, stride, bufferOffset + format.getOffset(j));
                    glEnableClientState(GL_NORMAL_ARRAY);
                    break;
                    
                case COLOR:
                    if(attr.getIndex() == 0)
                    {
                        glColorPointer(count, constant, stride, bufferOffset + format.getOffset(j));
                        glEnableClientState(GL_COLOR_ARRAY);
                    }
                    else
                    {
                        // secondary colors are added as generic attributes
                        glEnableVertexAttribArray(genericIndex);
                        glVertexAttribPointer(genericIndex++, count, constant, true, stride, bufferOffset + format.getOffset(j));
                    }
                    break;
                    
                case UV:
                {
                    if(attr.getIndex() == 1) OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                    glTexCoordPointer(count, constant, stride, bufferOffset + format.getOffset(j));
                    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
                    if(attr.getIndex() == 1) OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                    break;
                }
                
                case PADDING:
                    break;
                    
                case GENERIC:
                    glEnableVertexAttribArray(attr.getIndex());
                    glVertexAttribPointer(attr.getIndex(), count, constant, false, stride, bufferOffset + format.getOffset(j));
                    break;
                    
                default:
                    RenderHooks.INSTANCE.getLog().fatal("Unsupported attribute upload: {}", attr.getUsage().toString());
            }
        }
    }
}
