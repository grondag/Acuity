package grondag.acuity.buffering;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import grondag.acuity.Acuity;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.core.VertexPackingList;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.opengl.VaoStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

/**
 * Plays same role as VertexBuffer in RenderChunk but implementation
 * is much different.<p>
 * 
 * For solid layer, each pipeline will be separately collected
 * into memory-mapped buffers specific to that pipeline so that during
 * render we are able to render multiple chunks per pipeline out of 
 * the same buffer.<p>
 * 
 * For translucent layer, all pipelines will be collected into the 
 * same buffer because rendering order must be maintained.<p>
 * 
 * In both cases, it is possible for a pipeline's vertices to span
 * two buffers because our memory-mapped buffers are fixed size.<p>
 * 
 * The implementation handles the draw commands and vertex attribute 
 * state but relies on caller to manage shaders, uniforms, transforms
 * or any other GL state.<p>
 *
 *
 */
public abstract class DrawableChunk
{
    protected boolean isCleared = false;
    
    public int drawCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int quadCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    /**
     * Called when buffer content is no longer current and will not be rendered.
     */
    public void clear()
    {
        isCleared = true;
    }
    
    public static class Solid extends DrawableChunk
    {
        final ObjectArrayList<SolidDrawableChunkDelegate> delegates;
        
        public Solid(ObjectArrayList<SolidDrawableChunkDelegate> delegates)
        {
            this.delegates = delegates;
        }
        
        /**
         * Prepares for iteration and handles any internal housekeeping.
         * Called each frame from client thread before any call to {@link #renderSolidNext()}.
         */
        public void prepareSolidRender(Consumer<SolidDrawableChunkDelegate> consumer)
        {
            if(isCleared)
                return;
            
            final int limit = delegates.size();
            for(int i = 0; i < limit; i++)
                consumer.accept(delegates.get(i));
        }

        @Override
        public void clear()
        {
            super.clear();
            final int limit = delegates.size();
            for(int i = 0; i < limit; i++)
                delegates.get(i).release();
        }
    }
    
    public static class Translucent extends DrawableChunk
    {
        final IMappedBufferReference buffer;
        final VertexPackingList packing;
        final int bufferByteOffset;
        /**
         * VAO Buffer names if enabled and initialized.
         * Will be an entry for each entry in packing list - entries w/ same format will be repeated
         * but will have different offsets.
         * Set to null on release.
         */
        @Nullable int[] vaoBufferId = null;
        
        /**
         * Buffer offsets for each vao binding.
         */
        @Nullable int[] vaoVertexOffset = null;
        
        private static final int[] VAO_DISABLED = new int[0];
        
        public Translucent(IMappedBufferReference buffer, VertexPackingList packing, int bufferByteOffset)
        {
            this.buffer = buffer;
            this.packing = packing;
            this.bufferByteOffset = bufferByteOffset;
        }
        
        private void prepareVao()
        {
            if(vaoBufferId == null)
            {
                if(OpenGlHelperExt.isVaoEnabled())
                {
                    final int packCount = packing.size();
                    final int[] vaoBufferId = new int[packCount];
                    final int[] vaoVertexOffset = new int[packCount];
                    this.vaoBufferId = vaoBufferId;
                    this.vaoVertexOffset = vaoVertexOffset;

                    TextureFormat lastFormat = null;
                    int lastVaoBuffer = -1;
                    // start of each format within our buffer range
                    int formatByteOffset = bufferByteOffset;
                    // resets to 0 each new format
                    int pipelineVertexOffset = 0;
                    
                    for(int i = 0; i < packCount; i++)
                    {
                        final RenderPipeline p = packing.getPipeline(i);
                        final int vertexCount = packing.getCount(i);
                        final int stride = p.piplineVertexFormat().stride;
                        if(lastFormat != p.textureFormat)
                        {
                            lastFormat = p.textureFormat;
                            lastVaoBuffer = VaoStore.claimVertexArray();
                            pipelineVertexOffset = 0;
                            OpenGlHelperExt.glBindVertexArray(lastVaoBuffer);
                            GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                            OpenGlHelperExt.enableAttributesVao(p.piplineVertexFormat().attributeCount);
                            OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), stride, formatByteOffset);
                            p.piplineVertexFormat().bindAttributeLocations(formatByteOffset);
                        }
                       
                        vaoBufferId[i] = lastVaoBuffer;
                        vaoVertexOffset[i] = pipelineVertexOffset;
                        
                        pipelineVertexOffset += vertexCount;
                        formatByteOffset += vertexCount * stride;
                    }
                }
                else
                    vaoBufferId = VAO_DISABLED;
            }
        }
        
        public void renderChunkTranslucent()
        {
            if(isCleared || buffer.isDisposed())
                return;
            
            final VertexPackingList packing = this.packing;
            final IMappedBufferReference buffer = this.buffer;
            if(packing.size() == 0) return;
            buffer.bind();
            
            prepareVao();
            
            if(vaoBufferId == VAO_DISABLED)
                renderSlow();
            else
                renderFastVao();
        }

        private void renderFastVao()
        {
            final int[] vaoBufferId = this.vaoBufferId;
            final int[] vaoVertexOffset = this.vaoVertexOffset;
            if(vaoBufferId == null || vaoVertexOffset == null)
            {
                Acuity.INSTANCE.getLog().warn("Unable to render chunk due to null VAO data. This is a bug.");
                return;
            }
            
            final int packCount = packing.size();
            
            for(int i = 0; i < packCount; i++)
            {
                final RenderPipeline p = packing.getPipeline(i);
                final int vertexCount = packing.getCount(i);
                
                p.activate(false);
                
                if(i == 0 || vaoBufferId[i] != vaoBufferId[i - 1])
                    OpenGlHelperExt.glBindVertexArray(vaoBufferId[i]);
                
                OpenGlHelperExt.glDrawArraysFast(GL11.GL_QUADS, vaoVertexOffset[i], vertexCount);
            }
        }
        
        private void renderSlow()
        {
            final int packCount = packing.size();
            TextureFormat lastFormat = null;
            // start of each format within our buffer range
            int formatByteOffset = bufferByteOffset;
            // resets to 0 each new format
            int pipelineVertexOffset = 0;
            
            for(int i = 0; i < packCount; i++)
            {
                final RenderPipeline p = packing.getPipeline(i);
                final int vertexCount = packing.getCount(i);
                final int stride = p.piplineVertexFormat().stride;
                p.activate(false);
                
                if(lastFormat != p.textureFormat)
                {
                    lastFormat = p.textureFormat;
                    pipelineVertexOffset = 0;
                    OpenGlHelperExt.glVertexPointerFast(3, VertexFormatElement.EnumType.FLOAT.getGlConstant(), stride, formatByteOffset);
                    p.piplineVertexFormat().bindAttributeLocations(formatByteOffset);
                }
                
                OpenGlHelperExt.glDrawArraysFast(GL11.GL_QUADS, pipelineVertexOffset, vertexCount);
                
                pipelineVertexOffset += vertexCount;
                formatByteOffset += vertexCount * stride;
            }
        }
        
        @SuppressWarnings("null")
        @Override
        public void clear()
        {
            super.clear();
            buffer.release();
            if(vaoBufferId != null && vaoBufferId != VAO_DISABLED)
            {
                int lastId = -1;
                for(int b : vaoBufferId)
                {
                    if(lastId != b)
                    {
                        VaoStore.releaseVertexArray(b);
                        lastId = b;
                    }
                }
                vaoBufferId = null;
            }
        }
        
    }
}
