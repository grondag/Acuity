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

package grondag.acuity.core;

import java.util.function.Consumer;

import grondag.acuity.api.PipelineManagerImpl;
import grondag.acuity.buffering.DrawableChunkDelegate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SolidRenderCube implements Consumer<DrawableChunkDelegate>
{
    public final ObjectArrayList<DrawableChunkDelegate>[] pipelineLists;
    
    public SolidRenderCube()
    {
        final int size = PipelineManagerImpl.INSTANCE.pipelineCount();
        @SuppressWarnings("unchecked")
        ObjectArrayList<DrawableChunkDelegate>[] buffers = new ObjectArrayList[size];
        for(int i = 0; i < size; i++)
        {
            buffers[i] = new ObjectArrayList<DrawableChunkDelegate>();
        }
        this.pipelineLists = buffers;
    }

    @Override
    public void accept(DrawableChunkDelegate d)
    {
        pipelineLists[d.getPipeline().getIndex()].add(d);   
    }
}
