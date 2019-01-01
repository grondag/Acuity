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

/**
 * Provids access to the Render API runtime.<p>
 * 
 * You only need this if you want to register custom pipelines or
 * have special handling that depends on availability of render API
 * or render configuration.<p>
 *
 */
@Environment(EnvType.CLIENT)
public abstract class RenderRuntime
{
    static RenderRuntime instance = new RenderRuntime() {};
    
    /**
     * Will always return a non-null value.<p>
     * 
     * If Render API is missing will return a dummy implementation 
     * that reports {@link #isEnabled()} == false;
     */
    public static RenderRuntime getInstance()
    {
        return instance;
    }
    
    /**
     * Get this to register your pipelines and access the built-in pipelines.<p>
     * 
     * Will return null if Render API is missing.<p>
     * 
     * If the Render API is present but disabled, you will get the pipeline 
     * manager and be able to register pipelines.  They simply won't do anything
     * until the API is re-enabled.
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
     * True if the normal Minecraft lighting model is active.
     */
    public boolean isStandardLightingModel()
    {
        return true;
    }
    
    /**
     * Implement and register a listern if you need callbacks for status changes.<p>
     * 
     * Holds a weak reference, so no need to remove listeners that fall out of scope.
     */
    public void registerListener(RenderListener lister)
    {
        // NO OP
    }
}
