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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import grondag.acuity.Acuity;
import grondag.acuity.api.pipeline.PipelineManager;
import grondag.acuity.api.pipeline.PipelineManagerImpl;
import grondag.acuity.broken.PipelineHooks;
import grondag.acuity.buffer.MappedBufferStore;
import grondag.acuity.fermion.config.Localization;
import grondag.acuity.pipeline.PipelineShaderManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AcuityRuntimeImpl extends RenderRuntime
{
    public static final AcuityRuntimeImpl INSTANCE = new AcuityRuntimeImpl();
    
    public static void initialize()
    {
        RenderRuntime.instance = INSTANCE;
    }
    
    private ArrayList<WeakReference<RenderListener>> listeners = new ArrayList<WeakReference<RenderListener>>();
    
    private AcuityRuntimeImpl() {};
    
    
    @Override
    public final PipelineManager getPipelineManager()
    {
        return PipelineManagerImpl.INSTANCE;
    }

    @Override
    public final boolean isEnabled()
    {
        return Acuity.isModEnabled();
    }
    
    public void forceReload()
    {
        Acuity.INSTANCE.getLog().info(Localization.translate("misc.info_reloading"));
        Acuity.recomputeEnabledStatus();
        PipelineShaderManager.INSTANCE.forceReload();
        PipelineManagerImpl.INSTANCE.forceReload();
        PipelineHooks.forceReload();
        MappedBufferStore.forceReload();
        forEachListener(c -> c.onRenderReload());
    }

    @Override
    public void registerListener(RenderListener listener)
    {
        this.listeners.add(new WeakReference<RenderListener>(listener));
    }
    
    public void forEachListener(Consumer<RenderListener> c)
    {
        Iterator<WeakReference<RenderListener>> it = this.listeners.iterator();
        while(it.hasNext())
        {
            WeakReference<RenderListener> ref = it.next();
            RenderListener listener = ref.get();
            if(listener == null)
                it.remove();
            else
                c.accept(listener);
        }
    }
}
