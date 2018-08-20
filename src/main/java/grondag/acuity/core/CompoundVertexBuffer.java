package grondag.acuity.core;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import grondag.acuity.Acuity;
import grondag.acuity.Configurator;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.core.VertexPackingList.VertexPackingConsumer;
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
    
    private VertexPackingList vertexPackingList;
    
    public int drawCount()
    {
        return Acuity.isModEnabled() ?  this.vertexPackingList.size() : 1;
    }
    
    public int quadCount()
    {
        return Acuity.isModEnabled() ? this.vertexPackingList.quadCount() : this.count / 4;
    }
    
    private class VertexPackingRenderer extends VertexPackingConsumer
    {
        int bufferOffset = 0;
        int vertexOffset = 0;
        boolean isSolidLayer;
        
        @Nullable PipelineVertexFormat lastFormat = null;
        
        /**
         * Holds VAO buffer names.  Null if VAO not available.
         */
        @Nullable IntBuffer vaoNames = null;
        
        /**
         * Contents of {@link #vaoNames} as java array - faster access.
         * Int buffer is retained for ease of teardown.
         */
        @Nullable int[] vaoBufferId = null;
        
        /**
         * Bit flags to indicate if VAO for texture format is setup.
         * Reset to 0 when buffer is uploaded.
         */
        int vaoBindingFlags = 0;
        
        private VertexPackingRenderer()
        {
            if(OpenGlHelperExt.isVaoEnabled()) try
            {
                IntBuffer vao = BufferUtils.createIntBuffer(TextureFormat.values().length);
                OpenGlHelperExt.glGenVertexArrays(vao);
                this.vaoNames = vao;
                int[] vaoBufferId = new int[TextureFormat.values().length];
                
                OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, glBufferId);

                for(TextureFormat format : TextureFormat.values())
                {
                    final int bufferId = vao.get(format.ordinal());
                    vaoBufferId[format.ordinal()] = bufferId;
                    
                    // can set up everything except binding offsets for 2nd and 3rd pipeline
                    OpenGlHelperExt.glBindVertexArray(bufferId);
                    
                    GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                    
                    PipelineVertexFormat pvf = Configurator.lightingModel.vertexFormat(format);
                    OpenGlHelperExt.enableAttributesVao(pvf.attributeCount);
                    final int stride = pvf.stride; 
                    final int bufferOffset = 0;
                    OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), stride, bufferOffset);
                    
                    // UGLY: will wastefully bind secondary/tertiary layers
                    pvf.bindAttributeLocations(bufferOffset);
                    
                    //TODO: leave the base attributes interleaved and pack extended attributes at the end
                    //this will mean only the extended attributes ever have to be rebound
                    //but may complicate vertex offset tracking
                }
                
                OpenGlHelperExt.glBindVertexArray(0);
                OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
                this.vaoBufferId = vaoBufferId;
            }
            catch(Exception e)
            {
                // noop
            }
        }
        
        private void reset(boolean isSolidLayer)
        {
            bufferOffset = 0;
            vertexOffset = 0;
            lastFormat = null;
            this.isSolidLayer = isSolidLayer;
        }
        
        @SuppressWarnings("null")
        @Override
        public final void accept(RenderPipeline pipeline, int vertexCount)
        {
            pipeline.activate(isSolidLayer);
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
            int[] vao = this.vaoBufferId;
            if(vao == null)
                setupAttributesInner(format, bufferOffset);
            else
            {
                final int ordinal = format.layerIndex;
                int vaoName = vao[ordinal];
                OpenGlHelperExt.glBindVertexArray(vaoName);
                // single layer format never requires rebinding b/c always starts at 0
                if(ordinal > 0 && (this.vaoBindingFlags & (1 << ordinal)) == 0 )
                {
                    final int stride = format.stride;
                    OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), stride, bufferOffset);
                    format.bindAttributeLocations(bufferOffset);
                    this.vaoBindingFlags |= (1 << ordinal);
                }
            }
        }
        
        private void setupAttributesInner(PipelineVertexFormat format, int bufferOffset)
        {
            final int stride = format.stride;
            OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), stride, bufferOffset);
            format.enableAndBindAttributes(bufferOffset);
        }

        public final void deleteGlBuffers()
        {
            IntBuffer vao = this.vaoNames;
            if(vao != null) try
            {
                vao.position(0);
                OpenGlHelperExt.glDeleteVertexArrays(vao);
            }
            catch(Exception e)
            {
                // noop
            }
        }
    }
    
    private final VertexPackingRenderer vertexPackingConsumer = new VertexPackingRenderer();
    
    public CompoundVertexBuffer(VertexFormat     vertexFormatIn)
    {
        super(vertexFormatIn);
        this.vertexPackingList = new VertexPackingList();
    }

//    private static int maxSize = 0;
    
    public final void upload(ByteBuffer buffer, VertexPackingList packing)
    {
        this.vertexPackingList = packing;
        this.vertexPackingConsumer.vaoBindingFlags = 0;
        buffer.position(0);
//        int newMax = Math.max(maxSize, buffer.limit());
//        if(newMax > maxSize)
//        {
//            System.out.println("new max buffer size: " + newMax);
//            maxSize = newMax;
//        }
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }
    
    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();
        this.vertexPackingConsumer.deleteGlBuffers();
    }

    /**
     * Renders all uploaded vbos.
     */
    public final void renderChunk(boolean isSolidLayer)
    {
        final VertexPackingList packing = this.vertexPackingList;
        if(packing.size() == 0) return;
        
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
        vertexPackingConsumer.reset(isSolidLayer);
        
        packing.forEach(vertexPackingConsumer);
    }
}
