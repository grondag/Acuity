package grondag.acuity.core;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import grondag.acuity.api.RenderPipeline;
import grondag.acuity.core.VertexPackingList.IVertexPackingConsumer;
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
    
    public int drawCount()
    {
        final VertexPackingList vertexPackingList = this.vertexPackingList;
        return vertexPackingList == null ? 0 : vertexPackingList.size();
    }
    
    private class VertexPackingRenderer implements IVertexPackingConsumer
    {
        int bufferOffset = 0;
        int vertexOffset = 0;
        @Nullable PipelineVertexFormat lastFormat = null;
        
        private void reset()
        {
            bufferOffset = 0;
            vertexOffset = 0;
            lastFormat = null;
        }
        
        @SuppressWarnings("null")
        @Override
        public void accept(RenderPipeline pipeline, int vertexCount)
        {
            pipeline.activate();
            if(pipeline.piplineVertexFormat() != lastFormat)
            {
                vertexOffset = 0;
                lastFormat = pipeline.piplineVertexFormat();
                setupAttributes(lastFormat, bufferOffset);
            }
            
            OpenGlHelperExt.glDrawArraysFast(GL11.GL_QUADS, vertexOffset, vertexCount);
            
            vertexOffset += vertexCount;
            bufferOffset += vertexCount * lastFormat.stride;
        }
        
        private void setupAttributes(PipelineVertexFormat format, int bufferOffset)
        {
            final int stride = format.stride;
            OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), stride, bufferOffset);
            OpenGlHelperExt.glColorPointerFast(4, 5121, stride, bufferOffset + 12);
            OpenGlHelperExt.glTexCoordPointerFast(2, 5126, stride, bufferOffset + 16);
            OpenGlHelperExt.setClientActiveTextureFast(OpenGlHelper.lightmapTexUnit);
            OpenGlHelperExt.glTexCoordPointerFast(2, 5122, stride, bufferOffset + 24);
            OpenGlHelperExt.setClientActiveTextureFast(OpenGlHelper.defaultTexUnit);
            format.setupAttributes(bufferOffset);
        }
    }
    
    private final VertexPackingRenderer vertexPackingConsumer = new VertexPackingRenderer();
    
    public CompoundVertexBuffer(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);
    }

    public void upload(ByteBuffer buffer, VertexPackingList packing)
    {
        this.vertexPackingList = packing;
        buffer.position(0);
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }
    
    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
    }
    
    /**
     * Renders all uploaded vbos relying on OpenGl fixed function state.
     */
    public void renderChunk()
    {
        final VertexPackingList packing = this.vertexPackingList;
        if(packing == null || packing.size() == 0) return;
        
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        vertexPackingConsumer.reset();
        
        packing.forEach(vertexPackingConsumer);
    }

//    /**
//     * Remnant of a failed experiment
//     * Renders all uploaded vbos using the given model view matrix.
//     */
//    public void renderChunk(Matrix4f modelViewMatrix)
//    {
//        final VertexPackingList packing = this.vertexPackingList;
//        if(packing == null || packing.size() == 0) return;
//        
//        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
//        vertexPackingConsumer.reset();
//        packing.forEach((RenderPipeline pipeline, int vertexCount) -> vertexPackingConsumer.accept(pipeline.updateModelViewMatrix(modelViewMatrix), vertexCount));
//    }
}
