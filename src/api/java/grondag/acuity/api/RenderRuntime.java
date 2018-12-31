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

package grondag.acuity.api;

import grondag.acuity.api.pipeline.PipelineManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class RenderRuntime
{
    static RenderRuntime instance = new RenderRuntime() {};
    
    public static RenderRuntime getInstance()
    {
        return instance;
    }
    
    /**
     * Get this to register your pipelines and access the built-in pipelines.
     */
    public PipelineManager getPipelineManager()
    {
        return null;
    }
    
    /**
     * Will be false if any part of ASM modifications failed, render API is missing
     * or if user has disabled advanced render features in configuration.
     */
    public boolean isEnabled()
    {
        return false;
    }
    
    /**
     * Use if you need callbacks for status changes.
     * Holds a weak reference, so no need to remove listeners that fall out of scope.
     */
    public void registerListener(RenderListener lister)
    {
        // NO OP
    }
}
