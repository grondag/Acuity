/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.buffer;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import grondag.acuity.api.pipeline.PipelineVertexFormat;
import grondag.acuity.api.pipeline.RenderPipelineImpl;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.opengl.VaoStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormatElement;

@Environment(EnvType.CLIENT)
public class DrawableChunkDelegate
{
    private MappedBufferDelegate bufferDelegate;
    private final RenderPipelineImpl pipeline;
    final int vertexCount;
    /**
     * VAO Buffer name if enabled and initialized.
     */
    int vaoBufferId = -1;
    boolean vaoNeedsRefresh = true;
    
    private boolean isReleased = false;
    
    public DrawableChunkDelegate(MappedBufferDelegate bufferDelegate, RenderPipelineImpl pipeline, int vertexCount)
    {
        this.bufferDelegate = bufferDelegate;
        this.pipeline = pipeline;
        this.vertexCount = vertexCount;
        bufferDelegate.retain(this);
    }
    
    public MappedBufferDelegate bufferDelegate()
    {
        return this.bufferDelegate;
    }
    
    public void replaceBufferDelegate(MappedBufferDelegate newDelegate)
    {
        // possible we have been released after rebuffer happened
        if(isReleased)
            newDelegate.release(this);
        else
        {
            this.bufferDelegate = newDelegate;
            vaoNeedsRefresh = true;
        }
    }
    
    /**
     * Instances that share the same GL buffer will have the same ID.
     * Allows sorting in solid layer to avoid rebinding buffers for draws that
     * will have the same vertex buffer and pipeline/format.
     */
    public int bufferId()
    {
        return this.bufferDelegate.glBufferId();
    }
    
    /**
     * The pipeline (and vertex format) associated with this delegate.
     */
    public RenderPipelineImpl getPipeline()
    {
        return this.pipeline;
    }
    
    /**
     * Won't bind buffer if this buffer same as last - will only do vertex attributes.
     * Returns the buffer Id that is bound, or input if unchanged.
     */
    public int bind(int lastBufferId)
    {
        if(this.bufferDelegate.isDisposed())
            return lastBufferId;
        
        if(this.bufferDelegate.glBufferId() != lastBufferId)
        {
            this.bufferDelegate.bind();
            lastBufferId = this.bufferDelegate.glBufferId();
        }
        
        if(vaoNeedsRefresh)
        {
            final PipelineVertexFormat format = pipeline.textureDepth.vertexFormat;
            if(OpenGlHelperExt.isVaoEnabled())
            {
                if(vaoBufferId == -1)
                    vaoBufferId = VaoStore.claimVertexArray();
                OpenGlHelperExt.glBindVertexArray(vaoBufferId);
                GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
                OpenGlHelperExt.enableAttributesVao(format.attributeCount);
                bindVertexAttributes(format);
                return lastBufferId;
            }
            vaoNeedsRefresh = false;
        }
        
        if(vaoBufferId > 0)
            OpenGlHelperExt.glBindVertexArray(vaoBufferId);
        else
            bindVertexAttributes(pipeline.textureDepth.vertexFormat);
        
        return lastBufferId; 
       
    }
    
    private void bindVertexAttributes(PipelineVertexFormat format)
    {
        GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.stride, bufferDelegate.byteOffset());
        format.bindAttributeLocations(bufferDelegate.byteOffset());
    }
    
    /**
     * Assumes pipeline has already been activated and buffer has already been bound via {@link #bind()}
     */
    public void draw()
    {
        assert !isReleased;
        
        if(this.bufferDelegate.isDisposed())
            return;
        GlStateManager.drawArrays(GL11.GL_QUADS, 0, vertexCount);
    }
    
    public void release()
    {
        if(!isReleased)
        {
            isReleased = true;
            bufferDelegate.release(this);
            if(vaoBufferId != -1)
            {
                VaoStore.releaseVertexArray(vaoBufferId);
                vaoBufferId = -1;
            }
        }
    }

    public void flush()
    {
        assert !isReleased;
        this.bufferDelegate.flush();
    }
}
