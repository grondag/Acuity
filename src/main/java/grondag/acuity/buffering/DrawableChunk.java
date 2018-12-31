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

package grondag.acuity.buffering;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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
@Environment(EnvType.CLIENT)
public abstract class DrawableChunk
{
    protected boolean isCleared = false;
    
    protected ObjectArrayList<DrawableChunkDelegate> delegates;
    
    public DrawableChunk(ObjectArrayList<DrawableChunkDelegate> delegates)
    {
        this.delegates = delegates;
    }
    
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
    public final void clear()
    {
        if(!isCleared)
        {
            isCleared = true;
            assert delegates != null;
            if(!delegates.isEmpty())
            {
                final int limit = delegates.size();
                for(int i = 0; i < limit; i++)
                    delegates.get(i).release();
                delegates.clear();
            }
            DelegateLists.releaseDelegateList(delegates);
            delegates = null;
        }
    }
    
    @Override
    protected void finalize()
    {
        clear();
    }
    
    public static class Solid extends DrawableChunk
    {
        public Solid(ObjectArrayList<DrawableChunkDelegate> delegates)
        {
            super(delegates);
        }
        
        /**
         * Prepares for iteration and handles any internal housekeeping.
         * Called each frame from client thread before any call to {@link #renderSolidNext()}.
         */
        public void prepareSolidRender(Consumer<DrawableChunkDelegate> consumer)
        {
            if(isCleared)
                return;
            
            final int limit = delegates.size();
            for(int i = 0; i < limit; i++)
                consumer.accept(delegates.get(i));
        }
    }
    
    public static class Translucent extends DrawableChunk
    {
        public Translucent(ObjectArrayList<DrawableChunkDelegate> delegates)
        {
            super(delegates);
        }
        
        public void renderChunkTranslucent()
        {
            if(isCleared)
                return;
            
            final int limit = delegates.size();
            
            if(limit == 0)
                return;
            
            final Object[] draws = delegates.elements();

            int lastBufferId = -1;
            
            // using conventional loop here to prevent iterator garbage in hot loop
            // profiling shows it matters
            for(int i = 0; i < limit; i++)
            {
                final DrawableChunkDelegate b = (DrawableChunkDelegate)draws[i];
                b.getPipeline().activate(false);
                lastBufferId = b.bind(lastBufferId);
                b.draw();
            }
        }
    }
}
